package com.example.ieeeconnect.data.repository;

import com.example.ieeeconnect.domain.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventsRepository {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void loadEvents(Consumer<List<Event>> onLoaded) {
        firestore.collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Event> result = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) result.add(e);
                    }
                    onLoaded.accept(result);
                })
                .addOnFailureListener(e -> onLoaded.accept(new ArrayList<>()));
    }
}
