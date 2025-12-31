package com.example.ieeeconnect.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey
    @NonNull
    public String id;
    @NonNull
    public String chatId;
    @NonNull
    public String senderId;
    public String text;
    public String mediaUrl;
    public long sentAt;
}

