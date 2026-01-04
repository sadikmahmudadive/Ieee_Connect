package com.example.ieeeconnect.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.ieeeconnect.database.converters.ListToStringConverter;

import java.util.List;

@Entity(tableName = "events")
@TypeConverters(ListToStringConverter.class)
public class Event {
    @PrimaryKey
    @NonNull
    private String eventId;
    @NonNull
    private String title;
    @NonNull
    private String description;
    private long eventTime;
    @Nullable
    private String bannerUrl; // Cloudinary URL
    @Nullable
    private String createdByUserId;
    @NonNull
    private List<String> goingUserIds;
    @NonNull
    private List<String> interestedUserIds;

    public Event() {
        // Default constructor required for calls to DataSnapshot.getValue(Event.class)
    }

    public Event(@NonNull String eventId, @NonNull String title, @NonNull String description, long eventTime,
                 @Nullable String bannerUrl, @Nullable String createdByUserId, @NonNull List<String> goingUserIds,
                 @NonNull List<String> interestedUserIds) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.eventTime = eventTime;
        this.bannerUrl = bannerUrl;
        this.createdByUserId = createdByUserId;
        this.goingUserIds = goingUserIds;
        this.interestedUserIds = interestedUserIds;
    }

    @NonNull
    public String getEventId() { return eventId; }
    public void setEventId(@NonNull String eventId) { this.eventId = eventId; }

    @NonNull
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }

    @NonNull
    public String getDescription() { return description; }
    public void setDescription(@NonNull String description) { this.description = description; }

    public long getEventTime() { return eventTime; }
    public void setEventTime(long eventTime) { this.eventTime = eventTime; }

    @Nullable
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(@Nullable String bannerUrl) { this.bannerUrl = bannerUrl; }

    @Nullable
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(@Nullable String createdByUserId) { this.createdByUserId = createdByUserId; }

    @NonNull
    public List<String> getGoingUserIds() { return goingUserIds; }
    public void setGoingUserIds(@NonNull List<String> goingUserIds) { this.goingUserIds = goingUserIds; }

    @NonNull
    public List<String> getInterestedUserIds() { return interestedUserIds; }
    public void setInterestedUserIds(@NonNull List<String> interestedUserIds) { this.interestedUserIds = interestedUserIds; }
}
