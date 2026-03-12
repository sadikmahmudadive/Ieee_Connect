package com.example.ieeeconnect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatFragment extends Fragment {

    private FragmentChatHubBinding binding;
    private ChatHubViewModel viewModel;
    private ChatHubAdapter adapter;
    private List<ChatRoom> allRooms = new ArrayList<>();
    private boolean hasLoaded = false;

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
        setupSearch();
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
            if (binding == null) return;
            binding.loader.setVisibility(View.GONE);
            hasLoaded = true;
            allRooms = chatRooms != null ? chatRooms : new ArrayList<>();
            applySearchFilter();
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applySearchFilter() {
        String query = binding.etSearch.getText() != null
                ? binding.etSearch.getText().toString().trim().toLowerCase(Locale.getDefault())
                : "";

        List<ChatRoom> filtered;
        if (query.isEmpty()) {
            filtered = allRooms;
        } else {
            filtered = new ArrayList<>();
            for (ChatRoom room : allRooms) {
                String name = room.getRoomName() != null ? room.getRoomName().toLowerCase(Locale.getDefault()) : "";
                String msg = room.getLastMessage() != null ? room.getLastMessage().toLowerCase(Locale.getDefault()) : "";
                if (name.contains(query) || msg.contains(query)) {
                    filtered.add(room);
                }
            }
        }

        boolean isEmpty = filtered.isEmpty();
        binding.chatListRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        // Only show empty state after we've loaded data at least once
        binding.emptyState.setVisibility(isEmpty && hasLoaded ? View.VISIBLE : View.GONE);
        adapter.submitList(filtered);
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

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // When fragment becomes visible again, force adapter to rebind (reload avatars)
        if (!hidden && adapter != null && binding != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
