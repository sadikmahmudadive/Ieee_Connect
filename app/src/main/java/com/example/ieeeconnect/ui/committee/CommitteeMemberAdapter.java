package com.example.ieeeconnect.ui.committee;

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
import com.example.ieeeconnect.model.CommitteeMember;

public class CommitteeMemberAdapter extends ListAdapter<CommitteeMember, CommitteeMemberAdapter.MemberViewHolder> {

    public interface OnMemberActionListener {
        void onCallClick(CommitteeMember member);
        void onEmailClick(CommitteeMember member);
        void onItemClick(CommitteeMember member);
    }

    private final OnMemberActionListener listener;

    public CommitteeMemberAdapter(OnMemberActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<CommitteeMember> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CommitteeMember>() {
                @Override
                public boolean areItemsTheSame(@NonNull CommitteeMember oldItem, @NonNull CommitteeMember newItem) {
                    return oldItem.getUid() != null && oldItem.getUid().equals(newItem.getUid());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CommitteeMember oldItem, @NonNull CommitteeMember newItem) {
                    return oldItem.equals(newItem)
                            && safeEquals(oldItem.getName(), newItem.getName())
                            && safeEquals(oldItem.getRole(), newItem.getRole())
                            && safeEquals(oldItem.getPhotoUrl(), newItem.getPhotoUrl());
                }

                private boolean safeEquals(String a, String b) {
                    if (a == null && b == null) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_committee_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivMemberPhoto;
        private final TextView tvRole;
        private final TextView tvMemberName;
        private final TextView tvDesignation;
        private final TextView tvDepartment;
        private final ImageButton btnCall;
        private final ImageButton btnEmail;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMemberPhoto = itemView.findViewById(R.id.ivMemberPhoto);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvDesignation = itemView.findViewById(R.id.tvDesignation);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnEmail = itemView.findViewById(R.id.btnEmail);
        }

        void bind(CommitteeMember member) {
            tvMemberName.setText(member.getName());
            tvDesignation.setText(member.getDesignation());
            tvDepartment.setText(member.getDepartment());

            // Role badge
            String role = member.getRole();
            if (role != null && !role.isEmpty() && !"MEMBER".equalsIgnoreCase(role)) {
                tvRole.setVisibility(View.VISIBLE);
                tvRole.setText(member.getRoleDisplayName());

                // Color the badge based on role
                int badgeColor;
                switch (role.toUpperCase()) {
                    case "CHAIRMAN":
                        badgeColor = 0xFF1565C0; // Deep Blue
                        break;
                    case "VICE_CHAIRMAN":
                        badgeColor = 0xFF2E7D32; // Green
                        break;
                    case "SECRETARY":
                    case "JOINT_SECRETARY":
                        badgeColor = 0xFFE65100; // Orange
                        break;
                    case "TREASURER":
                        badgeColor = 0xFF6A1B9A; // Purple
                        break;
                    default:
                        badgeColor = 0xFF757575; // Grey
                        break;
                }
                tvRole.getBackground().setTint(badgeColor);
            } else {
                tvRole.setVisibility(View.GONE);
            }

            // Load photo with Glide
            String photoUrl = member.getPhotoUrl();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(ivMemberPhoto.getContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(ivMemberPhoto);
            } else {
                ivMemberPhoto.setImageResource(R.drawable.ic_profile_placeholder);
            }

            // Click listeners
            btnCall.setOnClickListener(v -> {
                if (listener != null) listener.onCallClick(member);
            });
            btnEmail.setOnClickListener(v -> {
                if (listener != null) listener.onEmailClick(member);
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(member);
            });
        }
    }
}

