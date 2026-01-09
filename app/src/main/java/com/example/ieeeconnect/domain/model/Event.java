package com.example.ieeeconnect.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.ieeeconnect.database.converters.ListToStringConverter;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "events")
@TypeConverters(ListToStringConverter.class)
public class Event {
    // Initialize @NonNull fields to avoid warnings
    @PrimaryKey
    @NonNull
    private String eventId = "";
    @NonNull
    private String title = "";
    @NonNull
    private String description = "";
    private long eventTime;
    @Nullable
    private String bannerUrl; // Cloudinary URL
    @Nullable
    private String createdByUserId;
    @NonNull
    private List<String> goingUserIds = new ArrayList<>();
    @NonNull
    private List<String> interestedUserIds = new ArrayList<>();
    @Nullable
    private String locationName;
    private long startTime;
    private long endTime;
    @Nullable
    private String id; // Firestore document id
    @Nullable
    private String location; // Firestore location field
    private long createdAt; // Firestore createdAt timestamp
    @Nullable
    private String category; // New field for event category

    public Event() {
        // Default constructor required for calls to DataSnapshot.getValue(Event.class)
    }

    public Event(@NonNull String eventId, @NonNull String title, @NonNull String description, long eventTime,
                 @Nullable String bannerUrl, @Nullable String createdByUserId, @NonNull List<String> goingUserIds,
                 @NonNull List<String> interestedUserIds, @Nullable String locationName, long startTime, long endTime,
                 @Nullable String id, @Nullable String location, long createdAt, @Nullable String category) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.eventTime = eventTime;
        this.bannerUrl = bannerUrl;
        this.createdByUserId = createdByUserId;
        this.goingUserIds = goingUserIds;
        this.interestedUserIds = interestedUserIds;
        this.locationName = locationName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.id = id;
        this.location = location;
        this.createdAt = createdAt;
        this.category = category;
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

    @Nullable
    public String getLocationName() { return locationName; }
    public void setLocationName(@Nullable String locationName) { this.locationName = locationName; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    @Nullable
    public String getId() { return id; }
    public void setId(@Nullable String id) { this.id = id; }
    @Nullable
    public String getLocation() { return location; }
    public void setLocation(@Nullable String location) { this.location = location; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Nullable
    public String getCategory() { return category; }
    public void setCategory(@Nullable String category) { this.category = category; }

    public String getFormattedTime() {
        // Example: Format startTime as a readable date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(startTime));
    }

    public int getLikes() {
        // Return the number of interested users as likes
        return interestedUserIds.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return eventId.equals(event.eventId) &&
                title.equals(event.title) &&
                description.equals(event.description) &&
                eventTime == event.eventTime &&
                startTime == event.startTime &&
                endTime == event.endTime &&
                ((bannerUrl == null && event.bannerUrl == null) || (bannerUrl != null && bannerUrl.equals(event.bannerUrl))) &&
                ((createdByUserId == null && event.createdByUserId == null) || (createdByUserId != null && createdByUserId.equals(event.createdByUserId))) &&
                ((locationName == null && event.locationName == null) || (locationName != null && locationName.equals(event.locationName))) &&
                goingUserIds.equals(event.goingUserIds) &&
                interestedUserIds.equals(event.interestedUserIds) &&
                ((id == null && event.id == null) || (id != null && id.equals(event.id))) &&
                ((location == null && event.location == null) || (location != null && location.equals(event.location))) &&
                createdAt == event.createdAt;
    }
}
