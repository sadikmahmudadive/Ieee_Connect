package com.example.ieeeconnect.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.ieeeconnect.data.repository.ChatRepository;
import com.example.ieeeconnect.domain.model.ChatRoom;
import com.example.ieeeconnect.domain.model.User;

import java.util.List;

public class ChatHubViewModel extends ViewModel {
    private final ChatRepository repository;
    private final LiveData<List<ChatRoom>> chatRooms;
    private final LiveData<List<User>> allUsers;

    public ChatHubViewModel() {
        this.repository = new ChatRepository();
        this.chatRooms = repository.getChatRooms();
        this.allUsers = repository.getAllUsers();
    }

    public LiveData<List<ChatRoom>> getChatRooms() {
        return chatRooms;
    }

    public LiveData<List<User>> getAllUsers() {
        return allUsers;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}
