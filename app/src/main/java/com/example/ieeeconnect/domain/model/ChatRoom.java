package com.example.ieeeconnect.domain.model;

import java.util.List;

public class ChatRoom {
    private String roomId;
    private String type; // "DIRECT" or "EVENT_GROUP"
    private List<String> participantIds;
    private String lastMessage;
    private long lastMessageTimestamp;
    private String eventId; // If it's an event group
    private String roomName; // For groups
    private String roomImage; // For groups

    public ChatRoom() {}

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomImage() { return roomImage; }
    public void setRoomImage(String roomImage) { this.roomImage = roomImage; }
}
