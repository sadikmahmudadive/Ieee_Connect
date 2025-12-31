package com.example.ieeeconnect;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.ieeeconnect.databinding.ActivityDashboardBinding;
import com.example.ieeeconnect.ui.events.EventsFragment;
import com.example.ieeeconnect.ui.home.HomeFragment;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                FirebaseAuth.getInstance().signOut();
                clearOnboardingFlag();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            return false;
        });

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                switchFragment(new HomeFragment());
                return true;
            }
            if (item.getItemId() == R.id.nav_events) {
                switchFragment(new EventsFragment());
                return true;
            }
            // placeholders for future tabs
            if (item.getItemId() == R.id.nav_community) {
                return true;
            }
            if (item.getItemId() == R.id.nav_chat) {
                return true;
            }
            if (item.getItemId() == R.id.nav_profile) {
                return true;
            }
            return false;
        });

        // default tab
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void clearOnboardingFlag() {
        getSharedPreferences("ieee_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_seen", false)
                .apply();
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentContainer, fragment);
        tx.commit();
    }
}
