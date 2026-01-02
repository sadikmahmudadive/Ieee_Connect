package com.example.ieeeconnect.model;

public class Message {
    private String id;
    private String fromId;
    private String toId; // either a user id or chat id
    private long timestamp;
    private String text;
    private String mediaUrl; // optional

    public Message() {}

    public Message(String id, String fromId, String toId, long timestamp, String text, String mediaUrl) {
        this.id = id;
        this.fromId = fromId;
        this.toId = toId;
        this.timestamp = timestamp;
        this.text = text;
        this.mediaUrl = mediaUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFromId() { return fromId; }
    public void setFromId(String fromId) { this.fromId = fromId; }
    public String getToId() { return toId; }
    public void setToId(String toId) { this.toId = toId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}

