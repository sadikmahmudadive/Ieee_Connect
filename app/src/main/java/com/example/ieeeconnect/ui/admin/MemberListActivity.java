package com.example.ieeeconnect.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberListActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvMemberCount;
    private EditText etSearch;
    private TabLayout tabFilter;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<MemberItem> allMembers = new ArrayList<>();
    private final List<MemberItem> pendingRequests = new ArrayList<>();
    private MemberAdapter adapter;
    private boolean showingPending = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_list);

        recycler = findViewById(R.id.member_list_recycler);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        etSearch = findViewById(R.id.etMemberSearch);
        tabFilter = findViewById(R.id.tabFilter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new MemberAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        // Setup tabs
        tabFilter.addTab(tabFilter.newTab().setText("All Members"));
        tabFilter.addTab(tabFilter.newTab().setText("Pending Requests"));

        String filter = getIntent().getStringExtra("filter");
        if ("pending_requests".equals(filter)) {
            tabFilter.selectTab(tabFilter.getTabAt(1));
            showingPending = true;
        }

        tabFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                showingPending = tab.getPosition() == 1;
                applyFilter();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadData();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        // Load members
        db.collection("users").get().addOnSuccessListener(snap -> {
            allMembers.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                MemberItem m = new MemberItem();
                m.uid = doc.getId();
                m.name = doc.getString("displayName");
                if (m.name == null || m.name.isEmpty()) m.name = doc.getString("name");
                if (m.name == null) m.name = "Member";
                m.email = doc.getString("email");
                m.photoUrl = doc.getString("photoUrl");
                m.role = doc.getString("role");
                Object isAdminObj = doc.get("isAdmin");
                m.isAdmin = isAdminObj instanceof Boolean && (Boolean) isAdminObj;
                m.isPending = false;
                allMembers.add(m);
            }

            // Load pending requests
            db.collection("join_requests").whereEqualTo("status", "pending").get()
                    .addOnSuccessListener(pSnap -> {
                        pendingRequests.clear();
                        for (DocumentSnapshot doc : pSnap.getDocuments()) {
                            MemberItem m = new MemberItem();
                            m.uid = doc.getId();
                            m.name = doc.getString("name");
                            if (m.name == null) m.name = "Unknown";
                            m.email = doc.getString("email");
                            m.role = "PENDING";
                            m.isPending = true;
                            pendingRequests.add(m);
                        }
                        progressBar.setVisibility(View.GONE);
                        applyFilter();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        applyFilter();
                    });
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load members", Toast.LENGTH_SHORT).show();
        });
    }

    private void applyFilter() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        List<MemberItem> source = showingPending ? pendingRequests : allMembers;
        List<MemberItem> filtered = new ArrayList<>();

        for (MemberItem m : source) {
            if (!query.isEmpty()) {
                boolean match = (m.name != null && m.name.toLowerCase().contains(query))
                        || (m.email != null && m.email.toLowerCase().contains(query))
                        || (m.role != null && m.role.toLowerCase().contains(query));
                if (!match) continue;
            }
            filtered.add(m);
        }

        adapter.setItems(filtered);
        tvMemberCount.setText(filtered.size() + " " + (showingPending ? "pending" : "members"));

        if (filtered.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(showingPending ? "No pending requests" : "No members found");
            recycler.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
    }

    // ── Change Role ─────────────────────────────────────────────

    private void showChangeRoleDialog(MemberItem member) {
        String[] roles = {"Member", "ADMIN", "EXCOM", "SUPER_ADMIN"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Role: " + member.name)
                .setItems(roles, (dialog, which) -> {
                    String newRole = roles[which];
                    boolean newAdmin = "ADMIN".equals(newRole) || "SUPER_ADMIN".equals(newRole);
                    Map<String, Object> update = new HashMap<>();
                    update.put("role", newRole);
                    update.put("isAdmin", newAdmin);
                    db.collection("users").document(member.uid).update(update)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, member.name + " → " + newRole, Toast.LENGTH_SHORT).show();
                                member.role = newRole;
                                member.isAdmin = newAdmin;
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRemoveMemberDialog(MemberItem member) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove Member")
                .setMessage("Remove " + member.name + " from the system? This will delete their user document.")
                .setPositiveButton("Remove", (d, w) -> {
                    db.collection("users").document(member.uid).delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, member.name + " removed", Toast.LENGTH_SHORT).show();
                                allMembers.remove(member);
                                applyFilter();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void approveRequest(MemberItem member) {
        // Create user doc and update join_request status
        Map<String, Object> userData = new HashMap<>();
        userData.put("displayName", member.name);
        userData.put("email", member.email);
        userData.put("role", "Member");
        userData.put("isAdmin", false);

        db.collection("users").document(member.uid).set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> {
                    db.collection("join_requests").document(member.uid).update("status", "approved");
                    Toast.makeText(this, member.name + " approved!", Toast.LENGTH_SHORT).show();
                    pendingRequests.remove(member);
                    applyFilter();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectRequest(MemberItem member) {
        db.collection("join_requests").document(member.uid).update("status", "rejected")
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, member.name + " rejected", Toast.LENGTH_SHORT).show();
                    pendingRequests.remove(member);
                    applyFilter();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Data model ──────────────────────────────────────────────

    static class MemberItem {
        String uid, name, email, photoUrl, role;
        boolean isAdmin, isPending;
    }

    // ── Adapter ─────────────────────────────────────────────────

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
        private List<MemberItem> items = new ArrayList<>();

        void setItems(List<MemberItem> list) {
            this.items = list;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_member, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.bind(items.get(pos)); }
        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView photo;
            TextView tvName, tvEmail, tvRole;
            ImageButton btnMenu;

            VH(@NonNull View v) {
                super(v);
                photo = v.findViewById(R.id.ivMemberPhoto);
                tvName = v.findViewById(R.id.tvMemberName);
                tvEmail = v.findViewById(R.id.tvMemberEmail);
                tvRole = v.findViewById(R.id.tvMemberRole);
                btnMenu = v.findViewById(R.id.btnMemberMenu);
            }

            void bind(MemberItem member) {
                tvName.setText(member.name);
                tvEmail.setText(member.email != null ? member.email : "");

                // Role badge
                if (member.role != null && !member.role.isEmpty() && !"Member".equalsIgnoreCase(member.role)) {
                    tvRole.setVisibility(View.VISIBLE);
                    tvRole.setText(member.role);
                    if (member.isPending) {
                        tvRole.getBackground().setTint(0xFFE65100); // orange
                    } else if ("ADMIN".equalsIgnoreCase(member.role) || "SUPER_ADMIN".equalsIgnoreCase(member.role)) {
                        tvRole.getBackground().setTint(0xFF1565C0); // blue
                    } else if ("EXCOM".equalsIgnoreCase(member.role)) {
                        tvRole.getBackground().setTint(0xFF2E7D32); // green
                    } else {
                        tvRole.getBackground().setTint(0xFF757575); // grey
                    }
                } else {
                    tvRole.setVisibility(View.GONE);
                }

                // Photo
                if (member.photoUrl != null && !member.photoUrl.isEmpty()) {
                    Glide.with(photo.getContext()).load(member.photoUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .circleCrop().into(photo);
                } else {
                    photo.setImageResource(R.drawable.ic_profile_placeholder);
                }

                // Menu
                btnMenu.setOnClickListener(v -> {
                    if (member.isPending) {
                        showPendingMenu(v, member);
                    } else {
                        showMemberMenu(v, member);
                    }
                });
            }

            private void showMemberMenu(View anchor, MemberItem member) {
                PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
                popup.getMenu().add(0, 1, 0, "Change Role");
                popup.getMenu().add(0, 2, 1, "Remove Member");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) { showChangeRoleDialog(member); return true; }
                    if (item.getItemId() == 2) { showRemoveMemberDialog(member); return true; }
                    return false;
                });
                popup.show();
            }

            private void showPendingMenu(View anchor, MemberItem member) {
                PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
                popup.getMenu().add(0, 1, 0, "✅ Approve");
                popup.getMenu().add(0, 2, 1, "❌ Reject");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) { approveRequest(member); return true; }
                    if (item.getItemId() == 2) { rejectRequest(member); return true; }
                    return false;
                });
                popup.show();
            }
        }
    }
}
