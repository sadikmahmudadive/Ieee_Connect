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
        diskIO.execute(() -> {
            List<Event> cached;
            try {
                cached = eventDao.getAllEvents();
            } catch (Exception e) {
                Log.w(TAG, "Error reading cached events", e);
                cached = new ArrayList<>();
            }
            allEvents.postValue(cached);
            fetchFromFirestore(ctx);
        });
    }

    public void refreshFromNetwork(Context ctx) {
        fetchFromFirestore(ctx);
    }

    private void fetchFromFirestore(Context ctx) {
        Log.d(TAG, "Fetching events from Firestore");
        firestore.collection("events").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Event> events = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Log.d(TAG, "Firestore doc: " + document.getId() + " => " + document.getData());
                    try {
                        Event event = document.toObject(Event.class);
                        if (event == null) continue;
                        event.setEventId(document.getId());
                        events.add(event);
                    } catch (Exception ex) {
                        Log.w(TAG, "Skipping invalid event document " + document.getId(), ex);
                    }
                }

                // Crucial fix: Always refresh local DB to match Firestore state
                diskIO.execute(() -> {
                    try {
                        eventDao.refreshEvents(events);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to refresh local DB", e);
                    }
                });

                Log.d(TAG, "Fetched " + events.size() + " events from Firestore");
                allEvents.postValue(events);
            } else {
                Log.w(TAG, "Failed to fetch events from Firestore", task.getException());
            }
        });
    }

    public void createEvent(Event event, Context context) {
        if (isNetworkAvailable(context)) {
            firestore.collection("events").add(event)
                    .addOnSuccessListener(docRef -> fetchFromFirestore(context))
                    .addOnFailureListener(e -> savePendingAndEnqueue(event, context));
        } else {
            savePendingAndEnqueue(event, context);
        }
    }

    private void savePendingAndEnqueue(Event event, Context context) {
        diskIO.execute(() -> {
            try {
                com.example.ieeeconnect.database.PendingEvent pe = new com.example.ieeeconnect.database.PendingEvent(event);
                pendingEventDao.insertPending(pe);
                OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(UploadPendingEventWorker.class).build();
                WorkManager.getInstance(context).enqueueUniqueWork("upload_pending_events", ExistingWorkPolicy.KEEP, work);
            } catch (Exception ex) {
                Log.w(TAG, "Failed to persist pending event", ex);
            }
        });
    }

    private boolean isNetworkAvailable(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @MainThread
    public void toggleGoingOptimistic(String eventId, String userId) {
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
        boolean wasGoing = target.getGoingUserIds().contains(userId);

        List<Event> updated = new ArrayList<>(current);
        Event copy = createEventCopy(target);

        if (wasGoing) {
            copy.getGoingUserIds().remove(userId);
        } else {
            if (!copy.getGoingUserIds().contains(userId)) copy.getGoingUserIds().add(userId);
            copy.getInterestedUserIds().remove(userId);
        }

        updated.set(idx, copy);
        allEvents.setValue(updated);

        diskIO.execute(() -> eventDao.refreshEvents(updated));

        if (wasGoing) {
            firestore.collection("events").document(eventId).update("goingUserIds", FieldValue.arrayRemove(userId));
        } else {
            firestore.collection("events").document(eventId).update("goingUserIds", FieldValue.arrayUnion(userId), "interestedUserIds", FieldValue.arrayRemove(userId));
        }
    }

    private Event createEventCopy(Event t) {
        Event c = new Event();
        c.setEventId(t.getEventId());
        c.setTitle(t.getTitle());
        c.setDescription(t.getDescription());
        c.setEventTime(t.getEventTime());
        c.setBannerUrl(t.getBannerUrl());
        c.setCreatedByUserId(t.getCreatedByUserId());
        c.setGoingUserIds(new ArrayList<>(t.getGoingUserIds()));
        c.setInterestedUserIds(new ArrayList<>(t.getInterestedUserIds()));
        c.setLocationName(t.getLocationName());
        c.setStartTime(t.getStartTime());
        c.setEndTime(t.getEndTime());
        c.setCategory(t.getCategory());
        return c;
    }

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

        List<Event> updated = new ArrayList<>(current);
        Event copy = createEventCopy(target);

        if (wasInterested) {
            copy.getInterestedUserIds().remove(userId);
        } else {
            if (!copy.getInterestedUserIds().contains(userId)) copy.getInterestedUserIds().add(userId);
            copy.getGoingUserIds().remove(userId);
        }

        updated.set(idx, copy);
        allEvents.setValue(updated);

        diskIO.execute(() -> eventDao.refreshEvents(updated));

        if (wasInterested) {
            firestore.collection("events").document(eventId).update("interestedUserIds", FieldValue.arrayRemove(userId));
        } else {
            firestore.collection("events").document(eventId).update("interestedUserIds", FieldValue.arrayUnion(userId), "goingUserIds", FieldValue.arrayRemove(userId));
        }
    }

    public LiveData<List<Event>> getUpcomingEvents(long currentTime) {
        MutableLiveData<List<Event>> upcomingEvents = new MutableLiveData<>();
        diskIO.execute(() -> {
            List<Event> events = eventDao.getUpcomingEvents(currentTime);
            upcomingEvents.postValue(events);
        });
        return upcomingEvents;
    }
}
