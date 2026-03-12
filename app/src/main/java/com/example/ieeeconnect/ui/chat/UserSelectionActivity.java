package com.example.ieeeconnect.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ieeeconnect.databinding.ActivityUserSelectionBinding;
import com.example.ieeeconnect.domain.model.ChatRoom;
import com.example.ieeeconnect.domain.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class UserSelectionActivity extends AppCompatActivity {

    private ActivityUserSelectionBinding binding;
    private ChatHubViewModel viewModel;
    private UserSelectionAdapter adapter;
    private List<User> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupViewModel();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new UserSelectionAdapter(this::startChatWithUser);
        binding.userListRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.userListRecycler.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        String query = binding.etSearchUsers.getText() != null
                ? binding.etSearchUsers.getText().toString().trim().toLowerCase(Locale.getDefault())
                : "";

        if (query.isEmpty()) {
            adapter.submitList(allUsers);
            return;
        }

        List<User> filtered = new ArrayList<>();
        for (User u : allUsers) {
            String name = u.getName() != null ? u.getName().toLowerCase(Locale.getDefault()) : "";
            String email = u.getEmail() != null ? u.getEmail().toLowerCase(Locale.getDefault()) : "";
            if (name.contains(query) || email.contains(query)) {
                filtered.add(u);
            }
        }
        adapter.submitList(filtered);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatHubViewModel.class);
        binding.loader.setVisibility(View.VISIBLE);

        viewModel.getAllUsers().observe(this, users -> {
            binding.loader.setVisibility(View.GONE);
            allUsers = users != null ? users : new ArrayList<>();
            applyFilter();
        });
    }

    private void startChatWithUser(User targetUser) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        // Create a deterministic ID for 1-on-1 chats to avoid duplicates
        String[] ids = {currentUserId, targetUser.getId()};
        Arrays.sort(ids);
        String roomId = "direct_" + ids[0] + "_" + ids[1];

        // Ensure the room exists in Firestore
        createRoomMetadataIfMissing(roomId, targetUser);

        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("roomName", targetUser.getName());
        intent.putExtra("roomImage", targetUser.getPhotoUrl());
        startActivity(intent);
        finish();
    }

    private void createRoomMetadataIfMissing(String roomId, User targetUser) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        ChatRoom newRoom = new ChatRoom();
                        newRoom.setRoomId(roomId);
                        newRoom.setType("DIRECT");
                        newRoom.setParticipantIds(Arrays.asList(currentUserId, targetUser.getId()));
                        newRoom.setRoomName(targetUser.getName());
                        newRoom.setRoomImage(targetUser.getPhotoUrl());
                        newRoom.setLastMessage("Start a conversation…");
                        newRoom.setLastMessageTimestamp(System.currentTimeMillis());

                        FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId).set(newRoom);
                    }
                });
    }
}
