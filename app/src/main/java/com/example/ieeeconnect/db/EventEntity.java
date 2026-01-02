package com.example.ieeeconnect.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String title;
    public String description;
    public long startTime;
    public long endTime;
    public String bannerUrl;
    public String creatorId;

    public EventEntity() {}
}
