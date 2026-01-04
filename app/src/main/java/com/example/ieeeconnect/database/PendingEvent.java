package com.example.ieeeconnect.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.ieeeconnect.domain.model.Event;

import java.util.UUID;

@Entity(tableName = "pending_events")
public class PendingEvent {
    @PrimaryKey
    @NonNull
    public String localId;

    // store serialized fields minimal for upload
    public String title;
    public String description;
    public long eventTime;
    public String bannerUrl;
    public String createdByUserId;

    public PendingEvent() {
        // ensure non-null localId for Room
        this.localId = UUID.randomUUID().toString();
    }

    public PendingEvent(Event e) {
        this.localId = UUID.randomUUID().toString();
        this.title = e.getTitle();
        this.description = e.getDescription();
        this.eventTime = e.getEventTime();
        this.bannerUrl = e.getBannerUrl();
        this.createdByUserId = e.getCreatedByUserId();
    }
}
