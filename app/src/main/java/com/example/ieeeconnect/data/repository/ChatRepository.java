package com.example.ieeeconnect.data.repository;

import androidx.annotation.NonNull;

import com.example.ieeeconnect.data.local.AppDatabase;
import com.example.ieeeconnect.data.local.MessageEntity;
import com.example.ieeeconnect.domain.model.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class ChatRepository {
    private final DatabaseReference chatsRef;
    private final AppDatabase db;
    private final Executor ioExecutor;

    public interface MessagesListener {
        void onMessages(List<Message> messages);
        void onError(Exception e);
    }

    public ChatRepository(@NonNull AppDatabase db, @NonNull Executor ioExecutor) {
        this.db = db;
        this.ioExecutor = ioExecutor;
        this.chatsRef = FirebaseDatabase.getInstance().getReference("chats");
    }

    public void listenForMessages(String chatId, MessagesListener listener) {
        // load cached first
        ioExecutor.execute(() -> {
            List<MessageEntity> cached = db.messageDao().getMessagesForChat(chatId);
            List<Message> mapped = mapEntities(cached);
            listener.onMessages(mapped);
        });

        chatsRef.child(chatId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messages = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message message = child.getValue(Message.class);
                    if (message != null) {
                        messages.add(message);
                    }
                }
                cacheMessages(messages);
                listener.onMessages(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }

    public void sendMessage(Message message) {
        chatsRef.child(message.getChatId()).child(message.getId()).setValue(message);
        cacheMessages(java.util.Collections.singletonList(message));
    }

    private void cacheMessages(List<Message> messages) {
        ioExecutor.execute(() -> {
            List<MessageEntity> entities = new ArrayList<>();
            for (Message m : messages) {
                MessageEntity entity = new MessageEntity();
                entity.id = m.getId();
                entity.chatId = m.getChatId();
                entity.senderId = m.getSenderId();
                entity.text = m.getText();
                entity.mediaUrl = m.getMediaUrl();
                entity.sentAt = m.getSentAt();
                entities.add(entity);
            }
            db.messageDao().upsertMessages(entities);
        });
    }

    private List<Message> mapEntities(List<MessageEntity> entities) {
        List<Message> messages = new ArrayList<>();
        for (MessageEntity e : entities) {
            messages.add(new Message(e.id, e.chatId, e.senderId, e.text, e.mediaUrl, e.sentAt));
        }
        return messages;
    }
}

