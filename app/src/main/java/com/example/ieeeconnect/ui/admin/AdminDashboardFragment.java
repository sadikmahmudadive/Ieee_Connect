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
import com.example.ieeeconnect.DashboardActivity;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.activities.CreateEventActivity;
import com.example.ieeeconnect.domain.model.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminDashboardFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        User currentUser = getCurrentUser();
        if (currentUser == null || !(currentUser.isAdmin() || isPrivilegedRole(currentUser.getRole()))) {
            verifyAdminFromServer(view);
            return view;
        }

        setupUI(view);
        return view;
    }

    private void setupUI(View view) {
        view.findViewById(R.id.btn_create_event).setOnClickListener(v -> navigateToCreateEvent());
        view.findViewById(R.id.btn_post_announcement).setOnClickListener(v -> navigateToPostAnnouncement());
        view.findViewById(R.id.btn_manage_members).setOnClickListener(v -> navigateToManageMembers());
        view.findViewById(R.id.btn_approve_requests).setOnClickListener(v -> navigateToApproveRequests());
        view.findViewById(R.id.btn_scan_attendance).setOnClickListener(v -> navigateToScanAttendance());
        view.findViewById(R.id.btn_broadcast_message).setOnClickListener(v -> navigateToBroadcastMessage());
        FloatingActionButton fab = view.findViewById(R.id.admin_dashboard_fab);
        fab.setOnClickListener(v -> navigateToBroadcastMessage());
    }

    private void verifyAdminFromServer(View view) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showAccessDenied();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
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
                        showAccessDenied();
                        return;
                    }
                    setupUI(view);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Failed to verify admin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showAccessDenied();
                });
    }

    /**
     * Instead of finishing the whole activity, navigate back to home.
     */
    private void showAccessDenied() {
        if (!isAdded()) return;
        Toast.makeText(getContext(), "Access Denied", Toast.LENGTH_SHORT).show();
        if (getActivity() instanceof DashboardActivity) {
            ((DashboardActivity) getActivity()).exitAdminMode();
        }
    }

    private User getCurrentUser() {
        Bundle args = getArguments();
        if (args == null) return null;
        boolean isAdmin = args.getBoolean("isAdmin", false);
        String role = args.getString("role", "");
        User user = new User();
        user.setId("");
        user.setName("");
        user.setEmail("");
        user.setAdmin(isAdmin);
        user.setRole(role);
        return user;
    }

    private boolean isPrivilegedRole(String role) {
        return "SUPER_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role) || "EXCOM".equalsIgnoreCase(role);
    }

    private void navigateToCreateEvent() {
        startActivity(new Intent(getContext(), CreateEventActivity.class));
    }

    private void navigateToPostAnnouncement() {
        showBroadcastDialog("Post Announcement", "announcement");
    }

    private void navigateToManageMembers() {
        startActivity(new Intent(getContext(), MemberListActivity.class));
    }

    private void navigateToApproveRequests() {
        // Show pending join requests from Firestore
        if (getContext() == null) return;
        Toast.makeText(getContext(), "Loading pending requests…", Toast.LENGTH_SHORT).show();

        FirebaseFirestore.getInstance().collection("join_requests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    int count = snap.size();
                    if (count == 0) {
                        Toast.makeText(getContext(), "No pending requests", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), count + " pending request(s) found", Toast.LENGTH_SHORT).show();
                        // Navigate to member list filtered for approvals
                        Intent intent = new Intent(getContext(), MemberListActivity.class);
                        intent.putExtra("filter", "pending_requests");
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to load requests", Toast.LENGTH_SHORT).show()
                );
    }

    private void navigateToScanAttendance() {
        startActivity(new Intent(getContext(), QrScannerActivity.class));
    }

    private void navigateToBroadcastMessage() {
        showBroadcastDialog("Broadcast Message", "broadcast");
    }

    private void showBroadcastDialog(String title, String type) {
        if (getContext() == null) return;


        // Create a proper input dialog
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("Enter your message…");
        input.setMinLines(3);
        input.setPadding(48, 32, 48, 16);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String message = input.getText() != null ? input.getText().toString().trim() : "";
                    if (message.isEmpty()) {
                        Toast.makeText(getContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendBroadcast(message, type);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendBroadcast(String message, String type) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("type", type);
        data.put("sentBy", uid);
        data.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("broadcasts")
                .add(data)
                .addOnSuccessListener(ref -> {
                    if (isAdded()) Toast.makeText(getContext(), "Message sent!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
