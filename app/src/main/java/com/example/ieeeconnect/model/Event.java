package com.example.ieeeconnect.model;

import java.util.List;

public class Event {
    private String id;
    private String title;
    private String description;
    private long startTime;
    private long endTime;
    private String bannerUrl;
    private String creatorId;
    private List<String> rsvpGoing;
    private List<String> rsvpInterested;

    public Event() {}

    public Event(String id, String title, String description, long startTime, long endTime, String bannerUrl, String creatorId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bannerUrl = bannerUrl;
        this.creatorId = creatorId;
    }

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public List<String> getRsvpGoing() { return rsvpGoing; }
    public void setRsvpGoing(List<String> rsvpGoing) { this.rsvpGoing = rsvpGoing; }
    public List<String> getRsvpInterested() { return rsvpInterested; }
    public void setRsvpInterested(List<String> rsvpInterested) { this.rsvpInterested = rsvpInterested; }
}

