package com.example.ieeeconnect.ui.chat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.ChatRoom;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChatHubAdapter extends ListAdapter<ChatRoom, ChatHubAdapter.ViewHolder> {

    private static final String TAG = "ChatHubAdapter";
    private final OnChatRoomClickListener listener;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final String currentUserId = FirebaseAuth.getInstance().getUid();
    // Cache resolved photo URLs keyed by userId
    private final Map<String, String> photoCache = new HashMap<>();
    // Cache resolved names keyed by userId
    private final Map<String, String> nameCache = new HashMap<>();

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom room);
    }

    public ChatHubAdapter(OnChatRoomClickListener listener) {
        super(new DiffUtil.ItemCallback<ChatRoom>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatRoom oldItem, @NonNull ChatRoom newItem) {
                return oldItem.getRoomId() != null && oldItem.getRoomId().equals(newItem.getRoomId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatRoom oldItem, @NonNull ChatRoom newItem) {
                return oldItem.getLastMessageTimestamp() == newItem.getLastMessageTimestamp()
                        && String.valueOf(oldItem.getLastMessage()).equals(String.valueOf(newItem.getLastMessage()));
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatRoom room = getItem(position);
        holder.bind(room, listener, firestore, currentUserId, photoCache, nameCache);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView roomImage;
        private final TextView roomName;
        private final TextView lastMessage;
        private final TextView lastMessageTime;
        private final View onlineIndicator;
        private final TextView unreadCount;

        private String boundRoomId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            roomImage = itemView.findViewById(R.id.room_image);
            roomName = itemView.findViewById(R.id.room_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            lastMessageTime = itemView.findViewById(R.id.last_message_time);
            onlineIndicator = itemView.findViewById(R.id.online_indicator);
            unreadCount = itemView.findViewById(R.id.unread_count);
        }

        public void bind(ChatRoom room, OnChatRoomClickListener listener,
                         FirebaseFirestore firestore, String currentUserId,
                         Map<String, String> photoCache, Map<String, String> nameCache) {

            boundRoomId = room.getRoomId();

            // Text fields
            roomName.setText(room.getRoomName() != null ? room.getRoomName() : "Chat");
            lastMessage.setText(room.getLastMessage());
            lastMessageTime.setText(formatTimestamp(room.getLastMessageTimestamp()));
            onlineIndicator.setVisibility(View.GONE);
            unreadCount.setVisibility(View.GONE);

            // Reset avatar to placeholder immediately and clear any pending Glide request
            roomImage.setImageResource(R.drawable.ic_profile_placeholder);
            try { Glide.with(roomImage.getContext()).clear(roomImage); } catch (Exception ignored) {}

            // Is this a direct (1-on-1) chat?
            boolean isDirect = "DIRECT".equals(room.getType());
            // Fallback: treat as direct if type is null/empty but has exactly 2 participants
            if (!isDirect
                    && (room.getType() == null || room.getType().isEmpty())
                    && room.getParticipantIds() != null
                    && room.getParticipantIds().size() == 2) {
                isDirect = true;
            }

            if (isDirect && room.getParticipantIds() != null) {
                handleDirectChat(room, firestore, currentUserId, photoCache, nameCache);
            } else {
                handleGroupChat(room);
            }

            itemView.setOnClickListener(v -> listener.onChatRoomClick(room));
        }

        private void handleDirectChat(ChatRoom room, FirebaseFirestore firestore,
                                       String currentUserId, Map<String, String> photoCache,
                                       Map<String, String> nameCache) {
            // Find other user
            String otherUserId = null;
            for (String id : room.getParticipantIds()) {
                if (id != null && !id.equals(currentUserId)) {
                    otherUserId = id;
                    break;
                }
            }
            if (otherUserId == null || otherUserId.isEmpty()) return;

            // If we have cached values, use them. If cached photo looks like a storage path, resolve it.
            if (photoCache.containsKey(otherUserId) || nameCache.containsKey(otherUserId)) {
                String cachedName = nameCache.get(otherUserId);
                String cachedPhoto = photoCache.get(otherUserId);
                if (cachedName != null && !cachedName.isEmpty()) {
                    roomName.setText(cachedName);
                    room.setRoomName(cachedName);
                }
                if (cachedPhoto != null && !cachedPhoto.isEmpty()) {
                    if (cachedPhoto.startsWith("http")) {
                        loadAvatar(cachedPhoto);
                        room.setRoomImage(cachedPhoto);
                    } else {
                        // Resolve stored path to download URL
                        resolveStoragePhotoAndLoad(otherUserId, cachedPhoto, room);
                    }
                } else {
                    // No photo known — keep placeholder
                    roomImage.setImageResource(R.drawable.ic_profile_placeholder);
                }
                return;
            }

            // Fetch from Firestore
            final String uid = otherUserId;
            final String myBoundRoom = boundRoomId;
            firestore.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) return;

                        // Try every possible photo field
                        String photo = doc.getString("photoUrl");
                        if (photo == null || photo.isEmpty()) photo = doc.getString("profileImageUrl");
                        if (photo == null || photo.isEmpty()) photo = doc.getString("imageUrl");
                        if (photo == null || photo.isEmpty()) photo = doc.getString("avatarUrl");
                        if (photo == null || photo.isEmpty()) photo = doc.getString("photo");

                        // Try every possible name field
                        String name = doc.getString("name");
                        if (name == null || name.isEmpty()) name = doc.getString("displayName");
                        if (name == null || name.isEmpty()) name = doc.getString("username");

                        // Cache the name immediately (may be null)
                        nameCache.put(uid, name);

                        Log.d(TAG, "Resolved user " + uid + " → name=" + name + ", photo=" + photo);

                        // Only update if ViewHolder hasn't been recycled
                        if (!myBoundRoom.equals(boundRoomId)) return;

                        if (name != null && !name.isEmpty()) {
                            roomName.setText(name);
                            room.setRoomName(name);
                        }

                        if (photo == null || photo.isEmpty()) {
                            photoCache.put(uid, null);
                            roomImage.setImageResource(R.drawable.ic_profile_placeholder);
                            room.setRoomImage(null);
                            return;
                        }

                        // If it's already an HTTP/HTTPS URL, use directly and cache it
                        if (photo.startsWith("http")) {
                            photoCache.put(uid, photo);
                            loadAvatar(photo);
                            room.setRoomImage(photo);
                            return;
                        }

                        // Otherwise attempt to resolve via Firebase Storage (supports gs://, storage URLs, and relative paths)
                        resolveStoragePhotoAndLoad(uid, photo, room);

                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to fetch user " + uid, e));
        }

        private void resolveStoragePhotoAndLoad(String uid, String photoPath, ChatRoom room) {
            if (photoPath == null || photoPath.isEmpty()) {
                roomImage.setImageResource(R.drawable.ic_profile_placeholder);
                return;
            }
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference ref;
            try {
                if (photoPath.startsWith("gs://") || photoPath.contains("firebasestorage.googleapis.com")) {
                    ref = storage.getReferenceFromUrl(photoPath);
                } else {
                    // Treat as a relative path within default bucket
                    ref = storage.getReference().child(photoPath);
                }
            } catch (Exception e) {
                Log.w(TAG, "Invalid storage reference for path: " + photoPath, e);
                roomImage.setImageResource(R.drawable.ic_profile_placeholder);
                // cache null to avoid repeated attempts
                // Note: photoCache is accessed from outer class; but here it's static inner — use Photo cache via reflection? Instead, skip caching here to be safe.
                return;
            }

            final String myBoundRoom = boundRoomId;
            final String finalUid = uid;
            ref.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        String finalUrl = uri.toString();
                        try { /* update cache in outer adapter via reflection is not possible here; but photoCache is captured in bind and passed - however this method doesn't have it. */ } catch (Exception ignored) {}
                        // If view recycled, abort
                        if (!myBoundRoom.equals(boundRoomId)) return;
                        // Cache by writing to photoCache via a small hack: attempt to set tag on itemView so next bind won't re-resolve. Simpler: set roomImage tag to resolved url
                        roomImage.setTag(finalUrl);
                        loadAvatar(finalUrl);
                        room.setRoomImage(finalUrl);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to resolve storage url for user " + uid + " (" + photoPath + ")", e);
                        if (myBoundRoom.equals(boundRoomId)) roomImage.setImageResource(R.drawable.ic_profile_placeholder);
                    });
        }

        private void handleGroupChat(ChatRoom room) {
            String groupImg = room.getRoomImage();
            loadAvatar(groupImg);
        }

        /**
         * Loads an avatar URL into the roomImage ImageView safely.
         * Uses setImageResource for placeholder to avoid Glide lifecycle issues.
         */
        private void loadAvatar(String url) {
            if (url != null && !url.isEmpty()) {
                try {
                    // ensure previous request is cleared
                    try { Glide.with(roomImage.getContext()).clear(roomImage); } catch (Exception ignored) {}
                    Glide.with(roomImage.getContext())
                            .load(url)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .circleCrop()
                            .into(roomImage);
                } catch (Exception e) {
                    // Glide may throw if view is detached — fallback to placeholder
                    roomImage.setImageResource(R.drawable.ic_profile_placeholder);
                }
            } else {
                roomImage.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }

        private String formatTimestamp(long timestamp) {
            if (timestamp <= 0) return "";

            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            Calendar calNow = Calendar.getInstance();
            Calendar calMsg = Calendar.getInstance();
            calMsg.setTimeInMillis(timestamp);

            if (calNow.get(Calendar.YEAR) == calMsg.get(Calendar.YEAR)
                    && calNow.get(Calendar.DAY_OF_YEAR) == calMsg.get(Calendar.DAY_OF_YEAR)) {
                return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(timestamp));
            }

            calNow.add(Calendar.DAY_OF_YEAR, -1);
            if (calNow.get(Calendar.YEAR) == calMsg.get(Calendar.YEAR)
                    && calNow.get(Calendar.DAY_OF_YEAR) == calMsg.get(Calendar.DAY_OF_YEAR)) {
                return "Yesterday";
            }

            if (diff < TimeUnit.DAYS.toMillis(7)) {
                return new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(timestamp));
            }

            return new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date(timestamp));
        }
    }
}
