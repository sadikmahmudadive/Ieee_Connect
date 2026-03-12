package com.example.ieeeconnect.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ieeeconnect.domain.model.ChatRoom;
import com.example.ieeeconnect.domain.model.Message;
import com.example.ieeeconnect.domain.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // Keep track of listeners so they can be cleaned up
    private ListenerRegistration roomsListener;
    private ListenerRegistration messagesListener;

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

        // Remove any previous listener before attaching a new one
        if (roomsListener != null) {
            roomsListener.remove();
        }

        roomsListener = firestore.collection("chat_rooms")
                .whereArrayContains("participantIds", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "getChatRooms error: " + error.getMessage());
                        roomsLiveData.setValue(new ArrayList<>());
                        return;
                    }
                    if (value != null) {
                        List<ChatRoom> rooms = value.toObjects(ChatRoom.class);
                        // Sort client-side by lastMessageTimestamp descending
                        // (avoids requiring a composite Firestore index)
                        Collections.sort(rooms, (a, b) ->
                                Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
                        roomsLiveData.setValue(rooms);
                    }
                });

        return roomsLiveData;
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
                    // Sort alphabetically by name
                    Collections.sort(users, Comparator.comparing(u -> u.getName().toLowerCase()));
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

    // Listen for messages in a specific room
    public void listenForMessages(String roomId, MessagesListener listener) {
        // Remove previous messages listener
        if (messagesListener != null) {
            messagesListener.remove();
        }

        messagesListener = firestore.collection("chat_rooms").document(roomId)
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

    // Send a message
    public void sendMessage(String roomId, Message message) {
        CollectionReference messagesRef = firestore.collection("chat_rooms")
                .document(roomId).collection("messages");

        String msgId = messagesRef.document().getId();
        message.setMessageId(msgId);

        messagesRef.document(msgId).set(message)
                .addOnSuccessListener(aVoid -> {
                    String preview;
                    if ("IMAGE".equals(message.getType())) {
                        preview = "\uD83D\uDCF7 Photo";
                    } else if ("AUDIO".equals(message.getType())) {
                        preview = "\uD83C\uDFA4 Voice note";
                    } else {
                        preview = message.getText();
                    }
                    firestore.collection("chat_rooms").document(roomId)
                            .update("lastMessage", preview,
                                    "lastMessageTimestamp", message.getTimestamp());
                });
    }

    /**
     * Clean up Firestore listeners. Call from ViewModel's onCleared().
     */
    public void cleanup() {
        if (roomsListener != null) {
            roomsListener.remove();
            roomsListener = null;
        }
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }
}
