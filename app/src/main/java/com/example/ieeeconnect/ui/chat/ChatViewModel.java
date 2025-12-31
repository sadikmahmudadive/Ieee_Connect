package com.example.ieeeconnect.ui.chat;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ieeeconnect.data.repository.ChatRepository;
import com.example.ieeeconnect.domain.model.Message;

import java.util.List;
import java.util.concurrent.Executor;

public class ChatViewModel extends ViewModel {
    private final ChatRepository repository;
    private final Executor mainExecutor;

    private final MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Throwable> errorLiveData = new MutableLiveData<>();

    public ChatViewModel(@NonNull ChatRepository repository, @NonNull Executor mainExecutor) {
        this.repository = repository;
        this.mainExecutor = mainExecutor;
    }

    public LiveData<List<Message>> getMessages() {
        return messagesLiveData;
    }

    public LiveData<Throwable> getError() {
        return errorLiveData;
    }

    public void start(String chatId) {
        repository.listenForMessages(chatId, new ChatRepository.MessagesListener() {
            @Override
            public void onMessages(List<Message> messages) {
                mainExecutor.execute(() -> messagesLiveData.setValue(messages));
            }

            @Override
            public void onError(Exception e) {
                mainExecutor.execute(() -> errorLiveData.setValue(e));
            }
        });
    }

    public void send(Message message) {
        repository.sendMessage(message);
    }
}

