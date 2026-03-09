package com.example.ieeeconnect.domain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {
    private String messageId;
    private String senderId;
    private String text;
    private String mediaUrl; // Cloudinary URL for Image/Voice
    private String type; // "TEXT", "IMAGE", "AUDIO"
    private long timestamp;
    private boolean isRead;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String messageId, String senderId, String text, String type, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
