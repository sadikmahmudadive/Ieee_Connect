package com.example.ieeeconnect.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ieeeconnect.models.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {
    private MutableLiveData<List<Message>> messages;
    private DatabaseReference chatRef;

    public ChatViewModel(String chatId) {
        messages = new MutableLiveData<>();
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);

        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Message> messageList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    messageList.add(message);
                }
                messages.setValue(messageList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public void sendMessage(Message message) {
        chatRef.push().setValue(message);
    }
}
