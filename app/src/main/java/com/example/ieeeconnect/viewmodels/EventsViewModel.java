package com.example.ieeeconnect.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.repositories.EventRepository;

import java.util.List;

public class EventsViewModel extends AndroidViewModel {
    private final EventRepository repository;
    private final LiveData<List<Event>> allEvents;
    private final LiveData<List<Event>> upcomingEvents;

    public EventsViewModel(@NonNull Application application) {
        super(application);
        repository = new EventRepository(application);
        allEvents = repository.getAllEvents();
        upcomingEvents = repository.getUpcomingEvents(System.currentTimeMillis());
    }

    public LiveData<List<Event>> getAllEvents() {
        return allEvents;
    }

    public LiveData<List<Event>> getUpcomingEvents() {
        return upcomingEvents;
    }

    // trigger a network refresh
    public void refreshFromNetwork() {
        if (repository != null) repository.refreshFromNetwork(getApplication());
    }

    // toggle RSVP states using optimistic updates
    public void toggleGoing(String eventId, String userId) {
        if (repository != null) repository.toggleGoingOptimistic(eventId, userId);
    }

    public void toggleInterested(String eventId, String userId) {
        if (repository != null) repository.toggleInterestedOptimistic(eventId, userId);
    }
}
