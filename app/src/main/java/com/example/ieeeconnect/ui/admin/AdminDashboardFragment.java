package com.example.ieeeconnect.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.activities.CreateEventActivity;
import com.example.ieeeconnect.domain.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        // Try to get admin info from args first
        User currentUser = getCurrentUser();
        if (currentUser == null || !(currentUser.isAdmin() || isPrivilegedRole(currentUser.getRole()))) {
            // If args don't indicate admin, double-check Firestore for current user doc
            verifyAdminFromServer(view);
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

    private void verifyAdminFromServer(View view) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    boolean isAdmin = false;
                    String role = "";
                    if (doc.exists()) {
                        Object isAdminObj = doc.get("isAdmin");
                        Object roleObj = doc.get("role");
                        if (isAdminObj instanceof Boolean) {
                            isAdmin = (Boolean) isAdminObj;
                        } else if (isAdminObj != null) {
                            String val = isAdminObj.toString().trim().toLowerCase();
                            isAdmin = val.equals("true") || val.equals("1") || val.equals("yes");
                        }
                        if (roleObj != null) role = roleObj.toString().trim();
                    }
                    if (!(isAdmin || isPrivilegedRole(role))) {
                        Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
                        requireActivity().finish();
                        return;
                    }
                    // If admin, set up UI handlers (repeat minimal setup)
                    view.findViewById(R.id.btn_create_event).setOnClickListener(v -> navigateToCreateEvent());
                    view.findViewById(R.id.btn_post_announcement).setOnClickListener(v -> navigateToPostAnnouncement());
                    view.findViewById(R.id.btn_manage_members).setOnClickListener(v -> navigateToManageMembers());
                    view.findViewById(R.id.btn_approve_requests).setOnClickListener(v -> navigateToApproveRequests());
                    view.findViewById(R.id.btn_scan_attendance).setOnClickListener(v -> navigateToScanAttendance());
                    view.findViewById(R.id.btn_broadcast_message).setOnClickListener(v -> navigateToBroadcastMessage());
                    FloatingActionButton fab = view.findViewById(R.id.admin_dashboard_fab);
                    fab.setOnClickListener(v -> navigateToBroadcastMessage());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to verify admin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    requireActivity().finish();
                });
    }

    private User getCurrentUser() {
        Bundle args = getArguments();
        if (args == null) return null;
        boolean isAdmin = args.getBoolean("isAdmin", false);
        String role = args.getString("role", "");
        // Minimal User object for access control
        User user = new User("", "", "", null, null);
        user.setAdmin(isAdmin);
        user.setRole(role);
        return user;
    }

    private boolean isPrivilegedRole(String role) {
        return "SUPER_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role) || "EXCOM".equalsIgnoreCase(role);
    }

    private void navigateToCreateEvent() {
        Intent intent = new Intent(getContext(), CreateEventActivity.class);
        startActivity(intent);
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
