package com.example.ieeeconnect.ui.chat;

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
import com.example.ieeeconnect.domain.model.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatHubAdapter extends ListAdapter<ChatRoom, ChatHubAdapter.ViewHolder> {

    private final OnChatRoomClickListener listener;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final String currentUserId = FirebaseAuth.getInstance().getUid();
    private final Map<String, String> userPhotoCache = new HashMap<>();

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom room);
    }

    public ChatHubAdapter(OnChatRoomClickListener listener) {
        super(new DiffUtil.ItemCallback<ChatRoom>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatRoom oldItem, @NonNull ChatRoom newItem) {
                return oldItem.getRoomId().equals(newItem.getRoomId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatRoom oldItem, @NonNull ChatRoom newItem) {
                return oldItem.getLastMessageTimestamp() == newItem.getLastMessageTimestamp() &&
                        String.valueOf(oldItem.getLastMessage()).equals(String.valueOf(newItem.getLastMessage()));
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
        holder.bind(room, listener, firestore, currentUserId, userPhotoCache);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView roomImage;
        private final TextView roomName;
        private final TextView lastMessage;
        private final TextView lastMessageTime;
        private final View onlineIndicator;
        private final TextView unreadCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            roomImage = itemView.findViewById(R.id.room_image);
            roomName = itemView.findViewById(R.id.room_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            lastMessageTime = itemView.findViewById(R.id.last_message_time);
            onlineIndicator = itemView.findViewById(R.id.online_indicator);
            unreadCount = itemView.findViewById(R.id.unread_count);
        }

        public void bind(ChatRoom room, OnChatRoomClickListener listener, FirebaseFirestore firestore, String currentUserId, Map<String, String> cache) {
            roomName.setText(room.getRoomName() != null ? room.getRoomName() : "Chat");
            lastMessage.setText(room.getLastMessage());
            
            if (room.getLastMessageTimestamp() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                lastMessageTime.setText(sdf.format(new Date(room.getLastMessageTimestamp())));
            } else {
                lastMessageTime.setText("");
            }

            if ("DIRECT".equals(room.getType()) && room.getParticipantIds() != null) {
                String otherUserId = "";
                for (String id : room.getParticipantIds()) {
                    if (!id.equals(currentUserId)) {
                        otherUserId = id;
                        break;
                    }
                }
                
                if (!otherUserId.isEmpty()) {
                    loadUserPhoto(otherUserId, firestore, cache);
                } else {
                    roomImage.setImageResource(R.drawable.ic_profile_placeholder);
                }
            } else {
                Glide.with(itemView.getContext())
                        .load(room.getRoomImage())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(roomImage);
            }

            if ("EVENT_GROUP".equals(room.getType())) {
                roomImage.setShapeAppearanceModel(roomImage.getShapeAppearanceModel().toBuilder()
                        .setAllCornerSizes(itemView.getContext().getResources().getDimension(R.dimen.margin_small))
                        .build());
            } else {
                roomImage.setShapeAppearanceModel(roomImage.getShapeAppearanceModel().toBuilder()
                        .setAllCornerSizes(500f) // Full circle
                        .build());
            }

            itemView.setOnClickListener(v -> listener.onChatRoomClick(room));
        }

        private void loadUserPhoto(String userId, FirebaseFirestore firestore, Map<String, String> cache) {
            if (cache.containsKey(userId)) {
                Glide.with(itemView.getContext())
                        .load(cache.get(userId))
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(roomImage);
            } else {
                firestore.collection("users").document(userId).get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            String photo = u.getPhotoUrl();
                            cache.put(userId, photo);
                            Glide.with(itemView.getContext())
                                    .load(photo)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .circleCrop()
                                    .into(roomImage);
                        }
                    }
                });
            }
        }
    }
}
