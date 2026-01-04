package com.example.ieeeconnect.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class Event {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String description;
    private String bannerUrl;
    private long eventTime;

    public Event(String title, String description, String bannerUrl, long eventTime) {
        this.title = title;
        this.description = description;
        this.bannerUrl = bannerUrl;
        this.eventTime = eventTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }
}
