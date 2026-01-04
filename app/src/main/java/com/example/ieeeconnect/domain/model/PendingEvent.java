package com.example.ieeeconnect.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class PendingEvent {
    private int id;
    @NonNull
    private String title = "";
    @NonNull
    private String description = "";
    private long eventTime;
    @Nullable
    private String bannerUrl;
    @Nullable
    private String createdByUserId;
    @NonNull
    private List<String> goingUserIds = new java.util.ArrayList<>();
    @NonNull
    private List<String> interestedUserIds = new java.util.ArrayList<>();
    @NonNull
    private String status = "pending";
    private long createdTimestamp;

    public PendingEvent() {}

    public PendingEvent(@NonNull String title, @NonNull String description, long eventTime,
                        @Nullable String bannerUrl, @Nullable String createdByUserId,
                        @NonNull List<String> goingUserIds, @NonNull List<String> interestedUserIds,
                        @NonNull String status, long createdTimestamp) {
        this.title = title;
        this.description = description;
        this.eventTime = eventTime;
        this.bannerUrl = bannerUrl;
        this.createdByUserId = createdByUserId;
        this.goingUserIds = goingUserIds;
        this.interestedUserIds = interestedUserIds;
        this.status = status;
        this.createdTimestamp = createdTimestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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

    @NonNull
    public String getStatus() { return status; }
    public void setStatus(@NonNull String status) { this.status = status; }

    public long getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(long createdTimestamp) { this.createdTimestamp = createdTimestamp; }
}
