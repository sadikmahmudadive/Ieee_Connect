package com.example.ieeeconnect.ui.chat;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.Message;
import com.example.ieeeconnect.domain.model.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends ListAdapter<Message, RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private final String currentUserId = FirebaseAuth.getInstance().getUid();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    
    // Cache for user profile images to avoid redundant Firestore calls
    private final Map<String, String> userPhotoCache = new HashMap<>();
    
    private MediaPlayer mediaPlayer;
    private int currentPlayingPosition = -1;

    public MessageAdapter() {
        super(new DiffUtil.ItemCallback<Message>() {
            @Override
            public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                return oldItem.getMessageId() != null && oldItem.getMessageId().equals(newItem.getMessageId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                return String.valueOf(oldItem.getText()).equals(String.valueOf(newItem.getText())) && 
                       oldItem.getTimestamp() == newItem.getTimestamp() &&
                       String.valueOf(oldItem.getMediaUrl()).equals(String.valueOf(newItem.getMediaUrl()));
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).getSenderId().equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = getItem(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message, position);
        } else {
            ((ReceivedViewHolder) holder).bind(message, position);
        }
    }

    class SentViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;
        private final TextView textTimestamp;
        private final ImageView imageStatus;
        private final ImageView imageContent;
        private final View voiceLayout;
        private final ImageButton btnPlay;
        private final SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());

        public SentViewHolder(@NonNull View view) {
            super(view);
            textMessage = view.findViewById(R.id.text_message);
            textTimestamp = view.findViewById(R.id.text_timestamp);
            imageStatus = view.findViewById(R.id.image_status);
            imageContent = view.findViewById(R.id.image_content);
            voiceLayout = view.findViewById(R.id.voice_layout);
            btnPlay = view.findViewById(R.id.btn_play_pause);
        }

        public void bind(Message message, int position) {
            boolean isVoice = "AUDIO".equals(message.getType());
            boolean isImage = "IMAGE".equals(message.getType());

            textMessage.setVisibility(message.getText() != null && !message.getText().isEmpty() ? View.VISIBLE : View.GONE);
            if (textMessage.getVisibility() == View.VISIBLE) textMessage.setText(message.getText());

            imageContent.setVisibility(isImage ? View.VISIBLE : View.GONE);
            if (isImage) Glide.with(itemView.getContext()).load(message.getMediaUrl()).into(imageContent);

            voiceLayout.setVisibility(isVoice ? View.VISIBLE : View.GONE);
            if (isVoice) {
                btnPlay.setImageResource(currentPlayingPosition == position ? R.drawable.ic_pause : R.drawable.ic_play);
                btnPlay.setOnClickListener(v -> playAudio(message.getMediaUrl(), position));
            }

            textTimestamp.setText(sdf.format(new Date(message.getTimestamp())));
            imageStatus.setImageResource(message.isRead() ? R.drawable.ic_check_double : R.drawable.ic_check);
        }
    }

    class ReceivedViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;
        private final TextView textTimestamp;
        private final ImageView imageContent;
        private final ShapeableImageView senderImage;
        private final View voiceLayout;
        private final ImageButton btnPlay;
        private final SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());

        public ReceivedViewHolder(@NonNull View view) {
            super(view);
            textMessage = view.findViewById(R.id.text_message);
            textTimestamp = view.findViewById(R.id.text_timestamp);
            imageContent = view.findViewById(R.id.image_content);
            senderImage = view.findViewById(R.id.sender_image);
            voiceLayout = view.findViewById(R.id.voice_layout);
            btnPlay = view.findViewById(R.id.btn_play_pause);
        }

        public void bind(Message message, int position) {
            boolean isVoice = "AUDIO".equals(message.getType());
            boolean isImage = "IMAGE".equals(message.getType());

            textMessage.setVisibility(message.getText() != null && !message.getText().isEmpty() ? View.VISIBLE : View.GONE);
            if (textMessage.getVisibility() == View.VISIBLE) textMessage.setText(message.getText());

            imageContent.setVisibility(isImage ? View.VISIBLE : View.GONE);
            if (isImage) Glide.with(itemView.getContext()).load(message.getMediaUrl()).into(imageContent);

            voiceLayout.setVisibility(isVoice ? View.VISIBLE : View.GONE);
            if (isVoice) {
                btnPlay.setImageResource(currentPlayingPosition == position ? R.drawable.ic_pause : R.drawable.ic_play);
                btnPlay.setOnClickListener(v -> playAudio(message.getMediaUrl(), position));
            }

            textTimestamp.setText(sdf.format(new Date(message.getTimestamp())));
            
            // Handle User Photo Fetching
            loadSenderPhoto(message.getSenderId());
        }

        private void loadSenderPhoto(String senderId) {
            if (userPhotoCache.containsKey(senderId)) {
                String url = userPhotoCache.get(senderId);
                Glide.with(itemView.getContext())
                        .load(url)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(senderImage);
            } else {
                // Fetch from Firestore
                firestore.collection("users").document(senderId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                User user = doc.toObject(User.class);
                                if (user != null) {
                                    String photoUrl = user.getPhotoUrl();
                                    userPhotoCache.put(senderId, photoUrl);
                                    Glide.with(itemView.getContext())
                                            .load(photoUrl)
                                            .placeholder(R.drawable.ic_profile_placeholder)
                                            .circleCrop()
                                            .into(senderImage);
                                }
                            }
                        });
            }
        }
    }

    private void playAudio(String url, int position) {
        if (currentPlayingPosition == position) {
            stopAudio();
            return;
        }

        stopAudio();
        currentPlayingPosition = position;
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(mp -> stopAudio());
            notifyItemChanged(position);
        } catch (IOException e) {
            Log.e("MessageAdapter", "Audio play failed", e);
        }
    }

    private void stopAudio() {
        int oldPos = currentPlayingPosition;
        currentPlayingPosition = -1;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (oldPos != -1) notifyItemChanged(oldPos);
    }
}
