package com.example.ieeeconnect;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.ieeeconnect.databinding.ActivityDashboardBinding;
import com.example.ieeeconnect.ui.admin.AdminDashboardFragment;
import com.example.ieeeconnect.ui.chat.ChatFragment;
import com.example.ieeeconnect.ui.committee.CommitteeFragment;
import com.example.ieeeconnect.ui.events.EventsFragment;
import com.example.ieeeconnect.ui.home.HomeFragment;
import com.example.ieeeconnect.ui.profile.ProfileFragment;
import com.example.ieeeconnect.ui.views.CustomBottomNavView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private ActivityDashboardBinding binding;

    // Cached fragment instances to prevent re-creation on every tab switch
    private final HomeFragment homeFragment = new HomeFragment();
    private final EventsFragment eventsFragment = new EventsFragment();
    private final ChatFragment chatFragment = new ChatFragment();
    private final CommitteeFragment committeeFragment = new CommitteeFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private AdminDashboardFragment adminFragment;
    private Fragment activeFragment;

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
            boolean isAdmin = getIntent().getBooleanExtra("isAdmin", false);
            String role = getIntent().getStringExtra("role");
            boolean isAdminRole = "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);
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

                            boolean privileged = docIsAdmin
                                    || "ADMIN".equalsIgnoreCase(docRole)
                                    || "SUPER_ADMIN".equalsIgnoreCase(docRole)
                                    || "EXCOM".equalsIgnoreCase(docRole);
                            navView.setAdminVisible(privileged);
                        } else {
                            Log.d(TAG, "No user document found for uid=" + uid);
                        }
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to fetch user role: " + e.getMessage()));
        }

        // Add all fragments initially, hide all except home
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, homeFragment, "home")
                    .add(R.id.fragment_container, eventsFragment, "events").hide(eventsFragment)
                    .add(R.id.fragment_container, chatFragment, "chat").hide(chatFragment)
                    .add(R.id.fragment_container, committeeFragment, "committee").hide(committeeFragment)
                    .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                    .commit();
            activeFragment = homeFragment;
        }

        navView.setOnNavigationItemSelectedListener(itemId -> {
            if (itemId == R.id.navigation_home) {
                switchTo(homeFragment);
            } else if (itemId == R.id.navigation_events) {
                switchTo(eventsFragment);
            } else if (itemId == R.id.navigation_chat) {
                switchTo(chatFragment);
            } else if (itemId == R.id.navigation_profile) {
                switchTo(profileFragment);
            } else if (itemId == R.id.navigation_committee) {
                switchTo(committeeFragment);
            } else if (itemId == R.id.navigation_admin) {
                if (adminFragment == null) {
                    adminFragment = new AdminDashboardFragment();
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, adminFragment, "admin")
                            .hide(adminFragment)
                            .commit();
                }
                switchTo(adminFragment);
            }
        });
    }

    /**
     * Show/hide fragments instead of replacing — preserves state, prevents
     * chat list reload, event list flash, etc.
     */
    private void switchTo(Fragment target) {
        if (target == activeFragment) return;
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.hide(activeFragment);
        tx.show(target);
        tx.commit();
        activeFragment = target;
    }

    /**
     * Called by AdminDashboardFragment when the user should leave admin mode.
     */
    public void exitAdminMode() {
        binding.customBottomNavView.setAdminVisible(false);
        if (activeFragment instanceof AdminDashboardFragment) {
            switchTo(homeFragment);
            binding.customBottomNavView.selectTab(0);
        }
    }
}
