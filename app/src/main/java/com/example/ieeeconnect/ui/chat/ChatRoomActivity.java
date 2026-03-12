package com.example.ieeeconnect.ui.chat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.data.remote.CloudinaryManager;
import com.example.ieeeconnect.data.remote.FCMClient;
import com.example.ieeeconnect.databinding.ActivityChatRoomBinding;
import com.example.ieeeconnect.domain.model.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatRoomActivity extends AppCompatActivity {

    private static final String TAG = "ChatRoomActivity";
    private static final int PERMISSION_RECORD_AUDIO = 31;
    private static final String FCM_SERVER_KEY = "YOUR_LEGACY_SERVER_KEY_HERE";

    private ActivityChatRoomBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private String roomId;
    private String roomName;
    private String roomImage;
    private String currentUserName = "Member";
    private String currentUserImage = "";

    private MediaRecorder recorder;
    private String audioPath;
    private long recordStartTime;
    private final Handler recordTimerHandler = new Handler(Looper.getMainLooper());
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private boolean isRecording = false;
    private boolean isUserAtBottom = true;

    private ListenerRegistration typingListener;
    private ListenerRegistration onlineListener;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadFile(uri, "IMAGE");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        roomId = getIntent().getStringExtra("roomId");
        roomName = getIntent().getStringExtra("roomName");
        roomImage = getIntent().getStringExtra("roomImage");

        if (roomId == null) {
            finish();
            return;
        }

        fetchCurrentUserInfo();
        setupToolbar();
        setupRecyclerView();
        setupViewModel();
        setupInputArea();
        setupScrollToBottom();
        listenForTypingIndicator();
        updateLastSeen();
    }

    // ── Current user info ───────────────────────────────────────

    private void fetchCurrentUserInfo() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            currentUserName = u.getName();
                            currentUserImage = u.getPhotoUrl();
                        }
                    }
                });
    }

    // ── Toolbar ─────────────────────────────────────────────────

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.roomName.setText(roomName != null ? roomName : "Chat");

        if (roomImage != null && !roomImage.isEmpty()) {
            Glide.with(this)
                    .load(roomImage)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.roomImage);
        } else {
            binding.roomImage.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Resolve online status for the other user in DIRECT chats
        resolveOnlineStatus();

        binding.btnAudioCall.setOnClickListener(v -> startCall(false));
        binding.btnVideoCall.setOnClickListener(v -> startCall(true));
    }

    private void resolveOnlineStatus() {
        FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String type = doc.getString("type");
                    if (!"DIRECT".equals(type)) {
                        binding.roomStatus.setText("Group chat");
                        return;
                    }

                    java.util.List<String> participants = (java.util.List<String>) doc.get("participantIds");
                    if (participants == null) return;
                    String myUid = FirebaseAuth.getInstance().getUid();
                    String otherUid = null;
                    for (String uid : participants) {
                        if (!uid.equals(myUid)) { otherUid = uid; break; }
                    }
                    if (otherUid == null) return;

                    // Listen for online status changes
                    DocumentReference userRef = FirebaseFirestore.getInstance()
                            .collection("users").document(otherUid);
                    onlineListener = userRef.addSnapshotListener((snap, error) -> {
                        if (snap != null && snap.exists()) {
                            Long lastSeen = snap.getLong("lastSeen");
                            if (lastSeen != null) {
                                long diff = System.currentTimeMillis() - lastSeen;
                                if (diff < 2 * 60 * 1000) { // within 2 min
                                    binding.roomStatus.setText("Active now");
                                    binding.roomStatus.setTextColor(getResources().getColor(R.color.primary, null));
                                } else if (diff < 60 * 60 * 1000) { // within 1 hour
                                    long mins = diff / (60 * 1000);
                                    binding.roomStatus.setText("Active " + mins + "m ago");
                                    binding.roomStatus.setTextColor(getResources().getColor(R.color.onSurfaceVariant, null));
                                } else {
                                    binding.roomStatus.setText("Offline");
                                    binding.roomStatus.setTextColor(getResources().getColor(R.color.onSurfaceVariant, null));
                                }
                            }
                        }
                    });
                });
    }

    // ── RecyclerView ────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerMessages.setLayoutManager(layoutManager);
        binding.recyclerMessages.setAdapter(adapter);
    }

    // ── ViewModel ───────────────────────────────────────────────

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.start(roomId);

        viewModel.getMessages().observe(this, messages -> {
            adapter.submitList(messages, () -> {
                if (isUserAtBottom && adapter.getItemCount() > 0) {
                    binding.recyclerMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                } else if (!isUserAtBottom) {
                    // Show scroll-to-bottom hint
                    binding.btnScrollBottom.setVisibility(View.VISIBLE);
                }
            });

            // Mark messages as read
            markMessagesAsRead();
        });

        viewModel.getError().observe(this, throwable -> {
            if (throwable != null) {
                Toast.makeText(this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Scroll to bottom ────────────────────────────────────────

    private void setupScrollToBottom() {
        binding.recyclerMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null) {
                    int lastVisible = lm.findLastCompletelyVisibleItemPosition();
                    int total = adapter.getItemCount();
                    isUserAtBottom = lastVisible >= total - 2;
                    binding.btnScrollBottom.setVisibility(isUserAtBottom ? View.GONE : View.VISIBLE);
                }
            }
        });

        binding.btnScrollBottom.setOnClickListener(v -> {
            if (adapter.getItemCount() > 0) {
                binding.recyclerMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
            binding.btnScrollBottom.setVisibility(View.GONE);
        });
    }

    // ── Input area ──────────────────────────────────────────────

    private void setupInputArea() {
        binding.btnSend.setImageResource(R.drawable.ic_mic);

        binding.editMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    binding.btnSend.setImageResource(R.drawable.ic_mic);
                } else {
                    binding.btnSend.setImageResource(R.drawable.ic_send);
                }
                // Set typing indicator
                setTyping(!s.toString().trim().isEmpty());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnSend.setOnTouchListener((v, event) -> {
            String text = binding.editMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    viewModel.sendMessage(text);
                    notifyOtherParticipants(text, "MESSAGE");
                    binding.editMessage.setText("");
                    setTyping(false);
                }
                return true;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopRecording();
                    return true;
            }
            return false;
        });

        binding.btnAttachment.setOnClickListener(v -> showAttachmentPicker());
    }

    // ── Typing indicator ────────────────────────────────────────

    private final Runnable clearTypingRunnable = () -> {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null && roomId != null) {
            FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId)
                    .update("typing_" + uid, false);
        }
    };

    private void setTyping(boolean typing) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || roomId == null) return;

        typingHandler.removeCallbacks(clearTypingRunnable);

        FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId)
                .update("typing_" + uid, typing);

        if (typing) {
            // Auto-clear typing after 3 seconds of no input
            typingHandler.postDelayed(clearTypingRunnable, 3000);
        }
    }

    private void listenForTypingIndicator() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        typingListener = FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId)
                .addSnapshotListener((snap, error) -> {
                    if (snap == null || !snap.exists()) return;

                    // Check all typing_<uid> fields
                    boolean someoneTyping = false;
                    String typerName = null;

                    Map<String, Object> data = snap.getData();
                    if (data != null) {
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            if (entry.getKey().startsWith("typing_") && !entry.getKey().equals("typing_" + myUid)) {
                                if (Boolean.TRUE.equals(entry.getValue())) {
                                    someoneTyping = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (someoneTyping) {
                        binding.typingIndicator.setText("typing…");
                        binding.typingIndicator.setVisibility(View.VISIBLE);
                    } else {
                        binding.typingIndicator.setVisibility(View.GONE);
                    }
                });
    }

    // ── Read receipts ───────────────────────────────────────────

    private void markMessagesAsRead() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null || roomId == null) return;

        FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        String senderId = doc.getString("senderId");
                        if (senderId != null && !senderId.equals(myUid)) {
                            doc.getReference().update("isRead", true);
                        }
                    }
                });
    }

    // ── Last seen ───────────────────────────────────────────────

    private void updateLastSeen() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("lastSeen", System.currentTimeMillis());
    }

    // ── Voice recording ─────────────────────────────────────────

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
            return;
        }

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) vibrator.vibrate(50);

        isRecording = true;
        recordStartTime = System.currentTimeMillis();
        audioPath = getExternalCacheDir().getAbsolutePath() + "/voice_note.aac";

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(audioPath);

        try {
            recorder.prepare();
            recorder.start();
            binding.editMessage.setHint("Recording... 0:00");
            updateRecordTimer();
        } catch (IOException e) {
            Log.e(TAG, "Recording start failed", e);
            isRecording = false;
        }
    }

    private void updateRecordTimer() {
        if (!isRecording) return;
        long elapsed = (System.currentTimeMillis() - recordStartTime) / 1000;
        binding.editMessage.setHint(String.format("Recording... %d:%02d", elapsed / 60, elapsed % 60));
        recordTimerHandler.postDelayed(this::updateRecordTimer, 1000);
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        recordTimerHandler.removeCallbacksAndMessages(null);
        binding.editMessage.setHint("Type a message...");

        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;

                long duration = System.currentTimeMillis() - recordStartTime;
                if (duration < 1000) {
                    new File(audioPath).delete();
                    Toast.makeText(this, "Hold to record", Toast.LENGTH_SHORT).show();
                } else {
                    uploadFile(Uri.fromFile(new File(audioPath)), "AUDIO");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "stopRecording failed", e);
        }
    }

    // ── Attachment picker ───────────────────────────────────────

    private void showAttachmentPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_attachment_picker, null);
        view.findViewById(R.id.btn_pick_image).setOnClickListener(v -> { dialog.dismiss(); pickImage(); });
        dialog.setContentView(view);
        dialog.show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    // ── File upload ─────────────────────────────────────────────

    private void uploadFile(Uri uri, String type) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        CloudinaryManager.upload(uri, new UploadCallback() {
            @Override
            public void onSuccess(String requestId, Map resultData) {
                String url = (String) resultData.get("secure_url");
                runOnUiThread(() -> {
                    if ("AUDIO".equals(type)) {
                        viewModel.sendVoiceMessage(url);
                        notifyOtherParticipants("Sent a voice note", "MESSAGE");
                    } else {
                        viewModel.sendImageMessage(url);
                        notifyOtherParticipants("Sent an image", "MESSAGE");
                    }
                });
            }
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onError(String requestId, ErrorInfo error) {
                runOnUiThread(() -> Toast.makeText(ChatRoomActivity.this, "Upload failed", Toast.LENGTH_SHORT).show());
            }
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        });
    }

    // ── Calls ───────────────────────────────────────────────────

    private void startCall(boolean isVideo) {
        notifyOtherParticipants("Incoming " + (isVideo ? "Video" : "Audio") + " call", "CALL");

        Intent intent = new Intent(this, CallScreenActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("roomName", roomName);
        intent.putExtra("roomImage", roomImage);
        intent.putExtra("isVideo", isVideo);
        startActivity(intent);
    }

    // ── Notifications ───────────────────────────────────────────

    private void notifyOtherParticipants(String messageBody, String type) {
        String myUid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        java.util.List<String> participants = (java.util.List<String>) doc.get("participantIds");
                        if (participants != null) {
                            for (String uid : participants) {
                                if (!uid.equals(myUid)) {
                                    sendFCMNotification(uid, messageBody, type);
                                }
                            }
                        }
                    }
                });
    }

    private void sendFCMNotification(String targetUserId, String body, String type) {
        if (FCM_SERVER_KEY.equals("YOUR_LEGACY_SERVER_KEY_HERE")) {
            Log.w(TAG, "sendFCMNotification: Server Key not set. Notification skipped.");
            return;
        }

        FirebaseFirestore.getInstance().collection("users").document(targetUserId).get()
                .addOnSuccessListener(doc -> {
                    String token = doc.getString("fcmToken");
                    if (token != null && !token.isEmpty()) {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Authorization", "key=" + FCM_SERVER_KEY);
                        headers.put("Content-Type", "application/json");

                        Map<String, Object> data = new HashMap<>();
                        data.put("type", type);
                        data.put("roomId", roomId);
                        data.put("senderName", currentUserName);
                        data.put("senderImage", currentUserImage);
                        data.put("message", body);

                        Map<String, Object> payload = new HashMap<>();
                        payload.put("to", token);
                        payload.put("data", data);
                        payload.put("priority", "high");

                        FCMClient.getClient().sendNotification(headers, payload).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                Log.d(TAG, response.isSuccessful() ? "Notification sent" : "Notification failed: " + response.code());
                            }
                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.e(TAG, "Notification error", t);
                            }
                        });
                    }
                });
    }

    // ── Lifecycle ───────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        updateLastSeen();
        markMessagesAsRead();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setTyping(false);
        updateLastSeen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recorder != null) { recorder.release(); recorder = null; }
        if (typingListener != null) typingListener.remove();
        if (onlineListener != null) onlineListener.remove();
        typingHandler.removeCallbacksAndMessages(null);
        recordTimerHandler.removeCallbacksAndMessages(null);
    }
}
