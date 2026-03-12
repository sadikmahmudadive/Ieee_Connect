package com.example.ieeeconnect.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ieeeconnect.data.repository.ChatRepository;
import com.example.ieeeconnect.domain.model.Message;

import java.util.List;

public class ChatViewModel extends ViewModel {
    private final ChatRepository repository;
    private final MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Throwable> errorLiveData = new MutableLiveData<>();
    private String currentRoomId;

    public ChatViewModel() {
        this.repository = new ChatRepository();
    }

    public LiveData<List<Message>> getMessages() {
        return messagesLiveData;
    }

    public LiveData<Throwable> getError() {
        return errorLiveData;
    }

    public void start(String roomId) {
        this.currentRoomId = roomId;
        repository.listenForMessages(roomId, new ChatRepository.MessagesListener() {
            @Override
            public void onMessages(List<Message> messages) {
                messagesLiveData.postValue(messages);
            }

            @Override
            public void onError(Exception e) {
                errorLiveData.postValue(e);
            }
        });
    }

    public void sendMessage(String text) {
        sendMessage(text, null, "TEXT");
    }

    public void sendImageMessage(String url) {
        sendMessage(null, url, "IMAGE");
    }

    public void sendVoiceMessage(String url) {
        sendMessage(null, url, "AUDIO");
    }

    private void sendMessage(String text, String mediaUrl, String type) {
        if (currentRoomId == null) return;

        Message message = new Message();
        message.setSenderId(com.google.firebase.auth.FirebaseAuth.getInstance().getUid());
        message.setText(text);
        message.setMediaUrl(mediaUrl);
        message.setType(type);
        message.setTimestamp(System.currentTimeMillis());

        repository.sendMessage(currentRoomId, message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}
