package com.example.ieeeconnect.ui.team;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the IEEE BUBT Student Branch executive team.
 * Fetches from Firestore "team_members" collection, falls back to sample data.
 */
public class TeamFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private TeamAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_team, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvTeamMembers);
        emptyText = view.findViewById(R.id.tvTeamEmpty);

        adapter = new TeamAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadTeamMembers();
    }

    private void loadTeamMembers() {
        FirebaseFirestore.getInstance().collection("team_members")
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    List<TeamMember> members = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        TeamMember m = doc.toObject(TeamMember.class);
                        if (m != null) members.add(m);
                    }
                    if (members.isEmpty()) members = getSampleTeam();
                    showMembers(members);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showMembers(getSampleTeam());
                });
    }

    private void showMembers(List<TeamMember> members) {
        adapter.setMembers(members);
        if (members.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private List<TeamMember> getSampleTeam() {
        List<TeamMember> list = new ArrayList<>();
        list.add(new TeamMember("Faculty Advisor", "Dr. Ahmed Rahman", "Professor, CSE", "", "ahmed@ieee.org", 1));
        list.add(new TeamMember("Chairperson", "Md. Karim Uddin", "CSE, Batch 45", "", "karim@ieee.org", 2));
        list.add(new TeamMember("Vice Chairperson", "Ayesha Siddiqua", "EEE, Batch 46", "", "ayesha@ieee.org", 3));
        list.add(new TeamMember("General Secretary", "Md. Tanvir Hasan", "CSE, Batch 46", "", "tanvir@ieee.org", 4));
        list.add(new TeamMember("Treasurer", "Tahira Khatun", "BBA, Batch 45", "", "tahira@ieee.org", 5));
        list.add(new TeamMember("Joint Secretary", "Sanjida Alam", "CSE, Batch 47", "", "sanjida@ieee.org", 6));
        list.add(new TeamMember("Webmaster", "Farhan Ahmed", "CSE, Batch 47", "", "farhan@ieee.org", 7));
        list.add(new TeamMember("Event Coordinator", "Sadia Jahan", "EEE, Batch 46", "", "sadia@ieee.org", 8));
        list.add(new TeamMember("Publicity Lead", "Nusrat Jahan", "EEE, Batch 47", "", "nusrat@ieee.org", 9));
        return list;
    }

    // ── Inner data class ────────────────────────────────────────

    public static class TeamMember {
        private String role;
        private String name;
        private String info;
        private String photoUrl;
        private String email;
        private int sortOrder;

        public TeamMember() {}

        public TeamMember(String role, String name, String info, String photoUrl, String email, int sortOrder) {
            this.role = role;
            this.name = name;
            this.info = info;
            this.photoUrl = photoUrl;
            this.email = email;
            this.sortOrder = sortOrder;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getInfo() { return info; }
        public void setInfo(String info) { this.info = info; }
        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    }

    // ── Adapter ─────────────────────────────────────────────────

    private class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.VH> {
        private List<TeamMember> members = new ArrayList<>();

        void setMembers(List<TeamMember> list) {
            this.members = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_member, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(members.get(position));
        }

        @Override
        public int getItemCount() { return members.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView photo;
            TextView tvName, tvRole, tvInfo;
            View btnEmail;

            VH(@NonNull View itemView) {
                super(itemView);
                photo = itemView.findViewById(R.id.ivTeamPhoto);
                tvName = itemView.findViewById(R.id.tvTeamName);
                tvRole = itemView.findViewById(R.id.tvTeamRole);
                tvInfo = itemView.findViewById(R.id.tvTeamInfo);
                btnEmail = itemView.findViewById(R.id.btnTeamEmail);
            }

            void bind(TeamMember member) {
                tvName.setText(member.getName());
                tvRole.setText(member.getRole());
                tvInfo.setText(member.getInfo());

                if (member.getPhotoUrl() != null && !member.getPhotoUrl().isEmpty()) {
                    Glide.with(photo.getContext())
                            .load(member.getPhotoUrl())
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .circleCrop()
                            .into(photo);
                } else {
                    photo.setImageResource(R.drawable.ic_profile_placeholder);
                }

                btnEmail.setOnClickListener(v -> {
                    if (member.getEmail() != null && !member.getEmail().isEmpty()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse("mailto:" + member.getEmail()));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "No email app found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }
}
