package com.example.ieeeconnect.repository;

import android.content.Context;

import com.example.ieeeconnect.db.AppDatabase;
import com.example.ieeeconnect.db.EventDao;
import com.example.ieeeconnect.db.EventEntity;
import com.example.ieeeconnect.model.Event;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventRepository {
    private final EventDao eventDao;
    private final FirebaseFirestore firestore;
    private final CollectionReference eventsRef;
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onComplete(List<Event> events);
        void onError(Exception e);
    }

    public EventRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        eventDao = db.eventDao();
        firestore = FirebaseFirestore.getInstance();
        eventsRef = firestore.collection("events");
    }

    public void getEventsCachedFirst(Callback callback) {
        // 1. return cached Room data quickly on background thread
        diskIO.execute(() -> {
            List<EventEntity> cached = eventDao.getAllEvents();
            List<Event> result = new ArrayList<>();
            for (EventEntity e : cached) {
                Event evt = new Event(e.id, e.title, e.description, e.startTime, e.endTime, e.bannerUrl, e.creatorId);
                result.add(evt);
            }
            if (!result.isEmpty()) {
                callback.onComplete(result);
            }

            // 2. fetch from Firestore and update local DB and callback
            eventsRef.orderBy("startTime").get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Event> fresh = new ArrayList<>();
                        List<EventEntity> toInsert = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Event e = doc.toObject(Event.class);
                            e.setId(doc.getId());
                            fresh.add(e);

                            EventEntity ent = new EventEntity();
                            ent.id = e.getId(); ent.title = e.getTitle(); ent.description = e.getDescription();
                            ent.startTime = e.getStartTime(); ent.endTime = e.getEndTime(); ent.bannerUrl = e.getBannerUrl(); ent.creatorId = e.getCreatorId();
                            toInsert.add(ent);
                        }
                        // insert into Room
                        diskIO.execute(() -> {
                            eventDao.insertEvents(toInsert);
                        });
                        callback.onComplete(fresh);
                    })
                    .addOnFailureListener(e -> {
                        if (result.isEmpty()) callback.onError(e);
                    });
        });
    }

    public void createEvent(Event event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        eventsRef.add(event)
                .addOnSuccessListener(documentReference -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }
}

