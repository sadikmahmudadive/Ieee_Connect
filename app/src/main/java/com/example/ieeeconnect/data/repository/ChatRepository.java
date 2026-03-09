package com.example.ieeeconnect.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ieeeconnect.domain.model.ChatRoom;
import com.example.ieeeconnect.domain.model.Message;
import com.example.ieeeconnect.domain.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface MessagesListener {
        void onMessages(List<Message> messages);
        void onError(Exception e);
    }

    public interface UserListener {
        void onUserFound(User user);
        void onError(Exception e);
    }

    // Get list of chat rooms for current user from Firestore
    public LiveData<List<ChatRoom>> getChatRooms() {
        MutableLiveData<List<ChatRoom>> roomsLiveData = new MutableLiveData<>();
        String currentUserId = FirebaseAuth.getInstance().getUid();
        
        if (currentUserId == null) {
            roomsLiveData.setValue(new ArrayList<>());
            return roomsLiveData;
        }

        firestore.collection("chat_rooms")
                .whereArrayContains("participantIds", currentUserId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "getChatRooms error: " + error.getMessage());
                        // Fallback if index is missing
                        fetchRoomsWithoutOrder(currentUserId, roomsLiveData);
                        return;
                    }
                    if (value != null) {
                        roomsLiveData.setValue(value.toObjects(ChatRoom.class));
                    }
                });
        return roomsLiveData;
    }

    private void fetchRoomsWithoutOrder(String uid, MutableLiveData<List<ChatRoom>> liveData) {
        firestore.collection("chat_rooms")
                .whereArrayContains("participantIds", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "fetchRoomsWithoutOrder error: " + error.getMessage());
                        liveData.setValue(new ArrayList<>());
                        return;
                    }
                    if (value != null) {
                        liveData.setValue(value.toObjects(ChatRoom.class));
                    }
                });
    }

    // Get all users for starting new chat
    public LiveData<List<User>> getAllUsers() {
        MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>();
        String currentUserId = FirebaseAuth.getInstance().getUid();

        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setId(doc.getId());
                            // Fallback for different field names in DB
                            if (user.getName() == null) user.setName(doc.getString("displayName"));
                            if (user.getPhotoUrl() == null) user.setPhotoUrl(doc.getString("profileImageUrl"));
                            
                            if (currentUserId != null && !user.getId().equals(currentUserId)) {
                                users.add(user);
                            }
                        }
                    }
                    usersLiveData.setValue(users);
                })
                .addOnFailureListener(e -> usersLiveData.setValue(new ArrayList<>()));
        return usersLiveData;
    }

    public void getUser(String userId, UserListener listener) {
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.setId(documentSnapshot.getId());
                        listener.onUserFound(user);
                    } else {
                        listener.onError(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    // Listen for messages in a specific room (NOW USING FIRESTORE SUB-COLLECTION)
    public void listenForMessages(String roomId, MessagesListener listener) {
        firestore.collection("chat_rooms").document(roomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (value != null) {
                        listener.onMessages(value.toObjects(Message.class));
                    }
                });
    }

    // Send a message (NOW WRITING TO FIRESTORE ONLY)
    public void sendMessage(String roomId, Message message) {
        CollectionReference messagesRef = firestore.collection("chat_rooms")
                .document(roomId).collection("messages");
        
        String msgId = messagesRef.document().getId();
        message.setMessageId(msgId);
        
        // 1. Save message to Firestore sub-collection
        messagesRef.document(msgId).set(message)
                .addOnSuccessListener(aVoid -> {
                    // 2. Update room metadata for the chat list view
                    firestore.collection("chat_rooms").document(roomId)
                            .update("lastMessage", message.getType().equals("IMAGE") ? "Photo" : 
                                    (message.getType().equals("AUDIO") ? "Voice note" : message.getText()),
                                    "lastMessageTimestamp", message.getTimestamp());
                });
    }
}
