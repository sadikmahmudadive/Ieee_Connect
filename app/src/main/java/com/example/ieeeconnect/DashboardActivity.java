package com.example.ieeeconnect;

import android.os.Bundle;

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

public class DashboardActivity extends AppCompatActivity {

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

        // Determine admin role from intent extras
        isAdmin = getIntent() != null && getIntent().getBooleanExtra("isAdmin", false);
        role = getIntent() != null ? getIntent().getStringExtra("role") : null;
        boolean isAdminRole = role != null && ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));

        // if user is admin, show admin tab
        if (isAdmin || isAdminRole) {
            navView.setAdminVisible(true);
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
