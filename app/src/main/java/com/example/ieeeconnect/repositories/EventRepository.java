package com.example.ieeeconnect.repositories;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.ieeeconnect.database.AppDatabase;
import com.example.ieeeconnect.database.EventDao;
import com.example.ieeeconnect.database.PendingEventDao;
import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.workers.UploadPendingEventWorker;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventRepository {
    private static final String TAG = "EventRepository";

    private final EventDao eventDao;
    private final PendingEventDao pendingEventDao;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<List<Event>> allEvents;
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();

    public EventRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        eventDao = db.eventDao();
        pendingEventDao = db.pendingEventDao();
        firestore = FirebaseFirestore.getInstance();
        allEvents = new MutableLiveData<>();

        loadEvents(application);
    }

    public LiveData<List<Event>> getAllEvents() {
        return allEvents;
    }

    private void loadEvents(Context ctx) {
        // Load cached events on diskIO, then postValue and trigger network fetch
        diskIO.execute(() -> {
            List<Event> cached;
            try {
                cached = eventDao.getAllEvents();
            } catch (Exception e) {
                Log.w(TAG, "Error reading cached events", e);
                cached = new ArrayList<>();
            }
            allEvents.postValue(cached);
            // After posting cached, fetch from network
            fetchFromFirestore(ctx);
        });
    }

    // public trigger to refresh from network
    public void refreshFromNetwork(Context ctx) {
        fetchFromFirestore(ctx);
    }

    private void fetchFromFirestore(Context ctx) {
        Log.d(TAG, "Fetching events from Firestore");
        firestore.collection("events").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Event> events = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Event event = document.toObject(Event.class);
                        if (event == null) continue;
                        event.setEventId(document.getId());
                        if (event.getCreatedByUserId() == null) {
                            event.setCreatedByUserId("");
                        }
                        if (event.getTitle() == null) event.setTitle("Untitled Event");
                        if (event.getDescription() == null) event.setDescription("");
                        if (event.getGoingUserIds() == null) event.setGoingUserIds(new ArrayList<>());
                        if (event.getInterestedUserIds() == null) event.setInterestedUserIds(new ArrayList<>());
                        events.add(event);
                    } catch (Exception ex) {
                        Log.w(TAG, "Skipping invalid event document " + document.getId(), ex);
                    }
                }

                // persist to Room on background thread
                diskIO.execute(() -> {
                    try {
                        if (!events.isEmpty()) {
                            eventDao.insertAll(events);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to persist events to local DB", e);
                    }
                });

                Log.d(TAG, "Fetched " + events.size() + " events from Firestore");
                allEvents.postValue(events);
            } else {
                Exception e = task.getException();
                Log.w(TAG, "Failed to fetch events from Firestore", e);
                if (allEvents.getValue() == null) {
                    allEvents.postValue(new ArrayList<>());
                }
            }
        });
    }

    /**
     * Create a new event. If network available, attempt to push to Firestore immediately.
     * If offline or upload fails, save to `pending_events` table and schedule a WorkManager job to upload later.
     */
    public void createEvent(Event event, Context context) {
        // try immediate upload
        if (isNetworkAvailable(context)) {
            firestore.collection("events").add(event)
                    .addOnSuccessListener(docRef -> {
                        // on success, fetch latest from network to refresh local DB/UI
                        fetchFromFirestore(context);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to create event online, saving to pending", e);
                        savePendingAndEnqueue(event, context);
                    });
        } else {
            Log.d(TAG, "No network - saving event to pending queue");
            savePendingAndEnqueue(event, context);
        }
    }

    private void savePendingAndEnqueue(Event event, Context context) {
        diskIO.execute(() -> {
            try {
                // insert pending event to DB
                com.example.ieeeconnect.database.PendingEvent pe = new com.example.ieeeconnect.database.PendingEvent(event);
                pendingEventDao.insertPending(pe);
                // enqueue a WorkManager job to upload pending events; use unique work to avoid duplicate floods
                OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(UploadPendingEventWorker.class)
                        .build();
                WorkManager.getInstance(context).enqueueUniqueWork("upload_pending_events", ExistingWorkPolicy.KEEP, work);
            } catch (Exception ex) {
                Log.w(TAG, "Failed to persist pending event", ex);
            }
        });
    }

    private boolean isNetworkAvailable(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Toggle Going status for a user on an event with optimistic UI update.
     * Replaces AsyncTask persistence with diskIO executor submissions.
     */
    @MainThread
    public void toggleGoingOptimistic(String eventId, String userId) {
        List<Event> current = allEvents.getValue();
        if (current == null) return;

        // find event index
        int idx = -1;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getEventId().equals(eventId)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return;

        Event target = current.get(idx);
        boolean wasGoing = target.getGoingUserIds().contains(userId);

        // create a copy of lists for mutation
        List<Event> updated = new ArrayList<>();
        for (Event e : current) updated.add(e);

        Event copy = new Event(target.getEventId(), target.getTitle(), target.getDescription(), target.getEventTime(), target.getBannerUrl(), target.getCreatedByUserId(), new ArrayList<>(target.getGoingUserIds()), new ArrayList<>(target.getInterestedUserIds()));

        if (wasGoing) {
            copy.getGoingUserIds().remove(userId);
        } else {
            if (!copy.getGoingUserIds().contains(userId)) copy.getGoingUserIds().add(userId);
            // also ensure removed from interested
            copy.getInterestedUserIds().remove(userId);
        }

        updated.set(idx, copy);
        // post optimistic update
        allEvents.setValue(updated);

        // persist change locally in background
        diskIO.execute(() -> {
            try {
                eventDao.insertAll(updated);
            } catch (Exception e) {
                Log.w(TAG, "Failed to persist optimistic change", e);
            }
        });

        // perform firestore atomic array updates
        if (wasGoing) {
            firestore.collection("events").document(eventId)
                    .update("goingUserIds", FieldValue.arrayRemove(userId))
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to remove going in firestore, reverting", e);
                        // revert UI
                        revertEventTo(eventId, target);
                    });
        } else {
            firestore.collection("events").document(eventId)
                    .update("goingUserIds", FieldValue.arrayUnion(userId), "interestedUserIds", FieldValue.arrayRemove(userId))
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to add going in firestore, reverting", e);
                        // revert UI
                        revertEventTo(eventId, target);
                    });
        }
    }

    @MainThread
    private void revertEventTo(String eventId, Event original) {
        List<Event> current = allEvents.getValue();
        if (current == null) return;
        List<Event> updated = new ArrayList<>();
        for (Event e : current) {
            if (e.getEventId().equals(eventId)) {
                updated.add(original);
            } else {
                updated.add(e);
            }
        }
        allEvents.setValue(updated);

        // persist revert
        diskIO.execute(() -> {
            try {
                eventDao.insertAll(updated);
            } catch (Exception ex) {
                Log.w(TAG, "Failed to persist revert", ex);
            }
        });
    }

    /**
     * Toggle Interested status with optimistic UI update.
     */
    @MainThread
    public void toggleInterestedOptimistic(String eventId, String userId) {
        List<Event> current = allEvents.getValue();
        if (current == null) return;

        int idx = -1;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getEventId().equals(eventId)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return;

        Event target = current.get(idx);
        boolean wasInterested = target.getInterestedUserIds().contains(userId);

        List<Event> updated = new ArrayList<>();
        for (Event e : current) updated.add(e);

        Event copy = new Event(target.getEventId(), target.getTitle(), target.getDescription(), target.getEventTime(), target.getBannerUrl(), target.getCreatedByUserId(), new ArrayList<>(target.getGoingUserIds()), new ArrayList<>(target.getInterestedUserIds()));

        if (wasInterested) {
            copy.getInterestedUserIds().remove(userId);
        } else {
            if (!copy.getInterestedUserIds().contains(userId)) copy.getInterestedUserIds().add(userId);
            // ensure removed from going
            copy.getGoingUserIds().remove(userId);
        }

        updated.set(idx, copy);
        allEvents.setValue(updated);

        diskIO.execute(() -> {
            try {
                eventDao.insertAll(updated);
            } catch (Exception e) {
                Log.w(TAG, "Failed to persist optimistic change", e);
            }
        });

        if (wasInterested) {
            firestore.collection("events").document(eventId)
                    .update("interestedUserIds", FieldValue.arrayRemove(userId))
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to remove interested in firestore, reverting", e);
                        revertEventTo(eventId, target);
                    });
        } else {
            firestore.collection("events").document(eventId)
                    .update("interestedUserIds", FieldValue.arrayUnion(userId), "goingUserIds", FieldValue.arrayRemove(userId))
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to add interested in firestore, reverting", e);
                        revertEventTo(eventId, target);
                    });
        }
    }
}
