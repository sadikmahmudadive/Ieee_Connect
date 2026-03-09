package com.example.ieeeconnect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ieeeconnect.databinding.FragmentChatHubBinding;
import com.example.ieeeconnect.domain.model.ChatRoom;

public class ChatFragment extends Fragment {

    private FragmentChatHubBinding binding;
    private ChatHubViewModel viewModel;
    private ChatHubAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatHubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupViewModel();
        setupListeners();
    }

    private void setupRecyclerView() {
        adapter = new ChatHubAdapter(room -> {
            Intent intent = new Intent(getActivity(), ChatRoomActivity.class);
            intent.putExtra("roomId", room.getRoomId());
            intent.putExtra("roomName", room.getRoomName());
            intent.putExtra("roomImage", room.getRoomImage());
            startActivity(intent);
        });
        binding.chatListRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.chatListRecycler.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatHubViewModel.class);
        binding.loader.setVisibility(View.VISIBLE);
        
        viewModel.getChatRooms().observe(getViewLifecycleOwner(), chatRooms -> {
            binding.loader.setVisibility(View.GONE);
            if (chatRooms != null && !chatRooms.isEmpty()) {
                adapter.submitList(chatRooms);
                binding.chatListRecycler.setVisibility(View.VISIBLE);
            } else {
                // If list is empty, show no chats yet
                binding.chatListRecycler.setVisibility(View.GONE);
            }
        });
    }

    private void setupListeners() {
        binding.fabNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UserSelectionActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
