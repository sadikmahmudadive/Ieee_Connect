package com.example.ieeeconnect.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieeeconnect.DashboardActivity;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.activities.CreateEventActivity;
import com.example.ieeeconnect.domain.model.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardFragment extends Fragment {

    private TextView statMembers, statEvents, statPending;
    private RecyclerView rvRecentActivity;
    private TextView tvActivityEmpty;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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
        // Stats
        statMembers = view.findViewById(R.id.stat_total_members);
        statEvents = view.findViewById(R.id.stat_total_events);
        statPending = view.findViewById(R.id.stat_pending_requests);
        rvRecentActivity = view.findViewById(R.id.rvRecentActivity);
        tvActivityEmpty = view.findViewById(R.id.tvActivityEmpty);

        rvRecentActivity.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Quick action buttons
        view.findViewById(R.id.btn_create_event).setOnClickListener(v -> navigateToCreateEvent());
        view.findViewById(R.id.btn_post_announcement).setOnClickListener(v -> navigateToPostAnnouncement());
        view.findViewById(R.id.btn_manage_members).setOnClickListener(v -> navigateToManageMembers());
        view.findViewById(R.id.btn_approve_requests).setOnClickListener(v -> navigateToApproveRequests());
        view.findViewById(R.id.btn_scan_attendance).setOnClickListener(v -> navigateToScanAttendance());
        view.findViewById(R.id.btn_broadcast_message).setOnClickListener(v -> navigateToBroadcastMessage());
        FloatingActionButton fab = view.findViewById(R.id.admin_dashboard_fab);
        fab.setOnClickListener(v -> navigateToBroadcastMessage());

        loadStats();
        loadRecentActivity();
    }

    // ── Live Stats ──────────────────────────────────────────────

    private void loadStats() {
        // Member count
        db.collection("users").get().addOnSuccessListener(snap -> {
            if (isAdded() && statMembers != null)
                statMembers.setText(String.valueOf(snap.size()));
        });
        // Event count
        db.collection("events").get().addOnSuccessListener(snap -> {
            if (isAdded() && statEvents != null)
                statEvents.setText(String.valueOf(snap.size()));
        });
        // Pending requests
        db.collection("join_requests").whereEqualTo("status", "pending").get()
                .addOnSuccessListener(snap -> {
                    if (isAdded() && statPending != null)
                        statPending.setText(String.valueOf(snap.size()));
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && statPending != null) statPending.setText("0");
                });
    }

    // ── Recent Activity ─────────────────────────────────────────

    private void loadRecentActivity() {
        db.collection("broadcasts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    List<ActivityItem> items = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String msg = doc.getString("message");
                        String type = doc.getString("type");
                        Long ts = doc.getLong("timestamp");
                        String time = ts != null ? sdf.format(new Date(ts)) : "";
                        String label = "broadcast".equals(type) ? "\uD83D\uDCE2 Broadcast" : "\uD83D\uDCE3 Announcement";
                        items.add(new ActivityItem(label, msg != null ? msg : "", time));
                    }
                    if (items.isEmpty()) {
                        tvActivityEmpty.setVisibility(View.VISIBLE);
                        rvRecentActivity.setVisibility(View.GONE);
                    } else {
                        tvActivityEmpty.setVisibility(View.GONE);
                        rvRecentActivity.setVisibility(View.VISIBLE);
                        rvRecentActivity.setAdapter(new ActivityAdapter(items));
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        tvActivityEmpty.setVisibility(View.VISIBLE);
                        rvRecentActivity.setVisibility(View.GONE);
                    }
                });
    }

    // ── Access Control ──────────────────────────────────────────

    private void verifyAdminFromServer(View view) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showAccessDenied();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    boolean isAdmin = false;
                    String role = "";
                    if (doc.exists()) {
                        Object isAdminObj = doc.get("isAdmin");
                        Object roleObj = doc.get("role");
                        if (isAdminObj instanceof Boolean) isAdmin = (Boolean) isAdminObj;
                        else if (isAdminObj != null) {
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
        boolean admin = args.getBoolean("isAdmin", false);
        String role = args.getString("role", "");
        User user = new User();
        user.setId("");
        user.setName("");
        user.setEmail("");
        user.setAdmin(admin);
        user.setRole(role);
        return user;
    }

    private boolean isPrivilegedRole(String role) {
        return "SUPER_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role) || "EXCOM".equalsIgnoreCase(role);
    }

    // ── Navigation ──────────────────────────────────────────────

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
        Intent intent = new Intent(getContext(), MemberListActivity.class);
        intent.putExtra("filter", "pending_requests");
        startActivity(intent);
    }

    private void navigateToScanAttendance() {
        startActivity(new Intent(getContext(), QrScannerActivity.class));
    }

    private void navigateToBroadcastMessage() {
        showBroadcastDialog("Broadcast Message", "broadcast");
    }

    private void showBroadcastDialog(String title, String type) {
        if (getContext() == null) return;
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

        db.collection("broadcasts").add(data)
                .addOnSuccessListener(ref -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Message sent!", Toast.LENGTH_SHORT).show();
                        loadRecentActivity(); // Refresh activity log
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Inner classes for recent activity ────────────────────────

    private static class ActivityItem {
        final String label, message, time;
        ActivityItem(String label, String message, String time) {
            this.label = label;
            this.message = message;
            this.time = time;
        }
    }

    private static class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.VH> {
        private final List<ActivityItem> items;
        ActivityAdapter(List<ActivityItem> items) { this.items = items; }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int pos) { holder.bind(items.get(pos)); }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView text1, text2;
            VH(@NonNull View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
            void bind(ActivityItem item) {
                text1.setText(item.label + "  •  " + item.time);
                text1.setTextSize(13);
                text2.setText(item.message);
                text2.setTextSize(14);
                text2.setMaxLines(2);
            }
        }
    }
}
