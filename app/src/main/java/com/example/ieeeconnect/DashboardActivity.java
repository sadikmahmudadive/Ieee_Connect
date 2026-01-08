package com.example.ieeeconnect;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.ieeeconnect.databinding.ActivityDashboardBinding;
import com.example.ieeeconnect.ui.admin.AdminDashboardFragment;
import com.example.ieeeconnect.ui.events.EventsFragment;
import com.example.ieeeconnect.ui.home.HomeFragment;
import com.example.ieeeconnect.ui.profile.ProfileFragment;
import com.example.ieeeconnect.ui.views.CustomBottomNavView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private ActivityDashboardBinding binding;
    private boolean isAdmin = false;
    private String role = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CustomBottomNavView navView = binding.customBottomNavView;

        // Default: hide admin tab until we determine the user's privileges
        navView.setAdminVisible(false);

        // First try: intent extras (backward-compatibility)
        if (getIntent() != null) {
            isAdmin = getIntent().getBooleanExtra("isAdmin", false);
            role = getIntent().getStringExtra("role");
            boolean isAdminRole = role != null && ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));
            if (isAdmin || isAdminRole) {
                navView.setAdminVisible(true);
            }
        }

        // Then attempt to determine current user's admin status from Firestore (preferred)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            boolean docIsAdmin = false;
                            String docRole = null;
                            Object isAdminObj = doc.get("isAdmin");
                            Object roleObj = doc.get("role");
                            if (isAdminObj instanceof Boolean) {
                                docIsAdmin = (Boolean) isAdminObj;
                            } else if (isAdminObj != null) {
                                String val = isAdminObj.toString().trim().toLowerCase();
                                docIsAdmin = val.equals("true") || val.equals("1") || val.equals("yes");
                            }
                            if (roleObj != null) docRole = roleObj.toString();

                            boolean privileged = docIsAdmin || (docRole != null && ("ADMIN".equalsIgnoreCase(docRole) || "SUPER_ADMIN".equalsIgnoreCase(docRole) || "EXCOM".equalsIgnoreCase(docRole)));
                            if (privileged) {
                                navView.setAdminVisible(true);
                            } else {
                                navView.setAdminVisible(false);
                            }
                        } else {
                            // fallback: roles/committee checks could be added if needed
                            Log.d(TAG, "No user document found for uid=" + uid);
                        }
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to fetch user role: " + e.getMessage()));
        }

        navView.setOnNavigationItemSelectedListener(itemId -> {
            if (itemId == R.id.navigation_home) {
                switchFragment(new HomeFragment());
            } else if (itemId == R.id.navigation_events) {
                switchFragment(new EventsFragment());
            } else if (itemId == R.id.navigation_chat) {
                // Chat fragment may be implemented elsewhere; for now stay on Home if missing
                try {
                    Class<?> cls = Class.forName("com.example.ieeeconnect.ui.chat.ChatFragment");
                    Fragment f = (Fragment) cls.newInstance();
                    switchFragment(f);
                } catch (Exception e) {
                    switchFragment(new HomeFragment());
                }
            } else if (itemId == R.id.navigation_profile) {
                switchFragment(new ProfileFragment());
            } else if (itemId == R.id.navigation_admin) {
                switchFragment(new AdminDashboardFragment());
            }
        });

        // Default selection -> Home
        if (savedInstanceState == null) {
            switchFragment(new HomeFragment());
        }
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragment_container, fragment);
        tx.commit();
    }

    public void exitAdminMode() {
        // Hide admin tab
        binding.customBottomNavView.setAdminVisible(false);
        // If currently showing AdminDashboardFragment, switch to HomeFragment
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current instanceof AdminDashboardFragment) {
            switchFragment(new HomeFragment());
            binding.customBottomNavView.selectTab(0);
        }
    }
}
