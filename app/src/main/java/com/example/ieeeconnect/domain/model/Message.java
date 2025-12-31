package com.example.ieeeconnect.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Message {
    @NonNull
    private String id;
    @NonNull
    private String chatId;
    @NonNull
    private String senderId;
    @Nullable
    private String text;
    @Nullable
    private String mediaUrl; // Cloudinary URL for images/files
    @NonNull
    private long sentAt;

    public Message(@NonNull String id, @NonNull String chatId, @NonNull String senderId,
                   @Nullable String text, @Nullable String mediaUrl, long sentAt) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.text = text;
        this.mediaUrl = mediaUrl;
        this.sentAt = sentAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getChatId() { return chatId; }
    public void setChatId(@NonNull String chatId) { this.chatId = chatId; }

    @NonNull
    public String getSenderId() { return senderId; }
    public void setSenderId(@NonNull String senderId) { this.senderId = senderId; }

    @Nullable
    public String getText() { return text; }
    public void setText(@Nullable String text) { this.text = text; }

    @Nullable
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(@Nullable String mediaUrl) { this.mediaUrl = mediaUrl; }

    public long getSentAt() { return sentAt; }
    public void setSentAt(long sentAt) { this.sentAt = sentAt; }
}

