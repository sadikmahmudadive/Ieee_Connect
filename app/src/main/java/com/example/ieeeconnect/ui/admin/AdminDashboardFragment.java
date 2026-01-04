package com.example.ieeeconnect.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AdminDashboardFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        // Example: Role-based access (should be checked in navigation logic as well)
        User currentUser = getCurrentUser();
        if (currentUser == null || !(currentUser.isAdmin() || isPrivilegedRole(currentUser.getRole()))) {
            Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
            requireActivity().finish(); // Use finish() instead of deprecated onBackPressed
            return view;
        }

        // Quick Actions
        view.findViewById(R.id.btn_create_event).setOnClickListener(v -> navigateToCreateEvent());
        view.findViewById(R.id.btn_post_announcement).setOnClickListener(v -> navigateToPostAnnouncement());
        view.findViewById(R.id.btn_manage_members).setOnClickListener(v -> navigateToManageMembers());
        view.findViewById(R.id.btn_approve_requests).setOnClickListener(v -> navigateToApproveRequests());
        view.findViewById(R.id.btn_scan_attendance).setOnClickListener(v -> navigateToScanAttendance());
        view.findViewById(R.id.btn_broadcast_message).setOnClickListener(v -> navigateToBroadcastMessage());
        FloatingActionButton fab = view.findViewById(R.id.admin_dashboard_fab);
        fab.setOnClickListener(v -> navigateToBroadcastMessage());

        // TODO: Load stats, activity log, etc.
        return view;
    }

    private User getCurrentUser() {
        // TODO: Replace with actual user session logic
        return null;
    }

    private boolean isPrivilegedRole(String role) {
        return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role) || "EXCOM".equals(role);
    }

    private void navigateToCreateEvent() {
        // TODO: Implement navigation
    }
    private void navigateToPostAnnouncement() {
        // TODO: Implement navigation
    }
    private void navigateToManageMembers() {
        // TODO: Implement navigation
    }
    private void navigateToApproveRequests() {
        // TODO: Implement navigation
    }
    private void navigateToScanAttendance() {
        // TODO: Implement navigation
    }
    private void navigateToBroadcastMessage() {
        // TODO: Implement navigation
    }
}
