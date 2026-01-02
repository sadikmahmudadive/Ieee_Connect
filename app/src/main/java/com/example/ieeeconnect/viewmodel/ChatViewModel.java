package com.example.ieeeconnect.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ieeeconnect.model.Message;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference chatRef;
    private ChildEventListener listener;

    public LiveData<List<Message>> getMessages() { return messages; }

    public void startListeningToChat(String chatId) {
        if (chatRef != null && listener != null) {
            chatRef.removeEventListener(listener);
        }
        chatRef = database.getReference("chats").child(chatId);
        List<Message> current = messages.getValue() != null ? messages.getValue() : new ArrayList<>();
        listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Message m = snapshot.getValue(Message.class);
                current.add(m);
                messages.postValue(current);
            }

            @Override public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(DataSnapshot snapshot) {}
            @Override public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(DatabaseError error) {}
        };
        chatRef.addChildEventListener(listener);
    }

    public void sendMessage(String chatId, Message message) {
        DatabaseReference ref = database.getReference("chats").child(chatId).push();
        ref.setValue(message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (chatRef != null && listener != null) chatRef.removeEventListener(listener);
    }
}

