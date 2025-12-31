package com.example.ieeeconnect.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class Event {
    @NonNull
    private String id;
    @NonNull
    private String title;
    @NonNull
    private String description;
    @NonNull
    private String startTimeIso;
    @Nullable
    private String bannerUrl; // Cloudinary URL
    @NonNull
    private String createdByUserId;
    @NonNull
    private List<String> goingUserIds;
    @NonNull
    private List<String> interestedUserIds;

    public Event(@NonNull String id, @NonNull String title, @NonNull String description, @NonNull String startTimeIso,
                 @Nullable String bannerUrl, @NonNull String createdByUserId, @NonNull List<String> goingUserIds,
                 @NonNull List<String> interestedUserIds) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTimeIso = startTimeIso;
        this.bannerUrl = bannerUrl;
        this.createdByUserId = createdByUserId;
        this.goingUserIds = goingUserIds;
        this.interestedUserIds = interestedUserIds;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }

    @NonNull
    public String getDescription() { return description; }
    public void setDescription(@NonNull String description) { this.description = description; }

    @NonNull
    public String getStartTimeIso() { return startTimeIso; }
    public void setStartTimeIso(@NonNull String startTimeIso) { this.startTimeIso = startTimeIso; }

    @Nullable
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(@Nullable String bannerUrl) { this.bannerUrl = bannerUrl; }

    @NonNull
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(@NonNull String createdByUserId) { this.createdByUserId = createdByUserId; }

    @NonNull
    public List<String> getGoingUserIds() { return goingUserIds; }
    public void setGoingUserIds(@NonNull List<String> goingUserIds) { this.goingUserIds = goingUserIds; }

    @NonNull
    public List<String> getInterestedUserIds() { return interestedUserIds; }
    public void setInterestedUserIds(@NonNull List<String> interestedUserIds) { this.interestedUserIds = interestedUserIds; }
}

