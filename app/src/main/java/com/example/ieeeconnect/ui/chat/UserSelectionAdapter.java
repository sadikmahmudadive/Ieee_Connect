package com.example.ieeeconnect.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.User;
import com.example.ieeeconnect.util.StorageImageLoader;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.HashMap;
import java.util.Map;

public class UserSelectionAdapter extends ListAdapter<User, UserSelectionAdapter.ViewHolder> {

    private final OnUserClickListener listener;
    private final Map<String, String> photoCache = new HashMap<>();

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserSelectionAdapter(OnUserClickListener listener) {
        super(new DiffUtil.ItemCallback<User>() {
            @Override
            public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getName().equals(newItem.getName()) &&
                        String.valueOf(oldItem.getPhotoUrl()).equals(String.valueOf(newItem.getPhotoUrl()));
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView userImage;
        private final TextView userName;
        private final TextView userEmail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            userEmail = itemView.findViewById(R.id.user_email);
        }

        public void bind(User user, OnUserClickListener listener) {
            userName.setText(user.getName());
            userEmail.setText(user.getEmail());
            // Use StorageImageLoader via adapter-level photoCache
            StorageImageLoader.load(userImage, user.getPhotoUrl(), UserSelectionAdapter.this.photoCache, user.getId(), R.drawable.ic_profile_placeholder);

            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }
    }
}
