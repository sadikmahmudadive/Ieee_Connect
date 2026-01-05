package com.example.ieeeconnect;

import android.os.Bundle;

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

public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CustomBottomNavView navView = binding.customBottomNavView;

        // Determine admin role from intent extras
        boolean isAdmin = getIntent() != null && getIntent().getBooleanExtra("isAdmin", false);
        String role = getIntent() != null ? getIntent().getStringExtra("role") : null;
        boolean isAdminRole = role != null && ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));

        // Show admin tab only for admin users
        navView.setAdminVisible(isAdmin || isAdminRole);

        navView.setOnTabSelectedListener(index -> {
            // When admin tab is visible the indices are 0..5 (admin at 4, profile at 5)
            switch (index) {
                case 0:
                    switchFragment(new HomeFragment());
                    break;
                case 1:
                    switchFragment(new EventsFragment());
                    break;
                case 2:
                    switchFragment(new ChatFragment());
                    break;
                case 3:
                    switchFragment(new CommitteeFragment());
                    break;
                case 4:
                    // If admin tab visible, index 4 = AdminDashboardFragment
                    if (navView.isAdminVisible()) {
                        AdminDashboardFragment adminFragment = new AdminDashboardFragment();
                        Bundle args = new Bundle();
                        args.putBoolean("isAdmin", true);
                        if (role != null) args.putString("role", role);
                        adminFragment.setArguments(args);
                        switchFragment(adminFragment);
                    } else {
                        // fallback to profile
                        switchFragment(new ProfileFragment());
                    }
                    break;
                case 5:
                    // profile when admin visible
                    switchFragment(new ProfileFragment());
                    break;
            }
        });

        // Default selection -> Home (also update nav visual state)
        navView.selectTab(0);

        // If launched with admin intent extras, navigate to AdminDashboardFragment directly
        if (isAdmin || isAdminRole) {
            AdminDashboardFragment fragment = new AdminDashboardFragment();
            Bundle args = new Bundle();
            args.putBoolean("isAdmin", true);
            if (role != null) args.putString("role", role);
            fragment.setArguments(args);
            switchFragment(fragment);
            // mark admin tab selected visually
            navView.selectTab(4);
        }
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragment_container, fragment);
        // Add to back stack only for AdminDashboardFragment
        if (fragment instanceof AdminDashboardFragment) {
            tx.addToBackStack(null);
        }
        tx.commit();
    }

    public void exitAdminMode() {
        // Hide admin tab
        binding.customBottomNavView.setAdminVisible(false);
        // If currently showing AdminDashboardFragment, switch to HomeFragment
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current instanceof com.example.ieeeconnect.ui.admin.AdminDashboardFragment) {
            switchFragment(new HomeFragment());
            binding.customBottomNavView.selectTab(0);
        }
    }
}
