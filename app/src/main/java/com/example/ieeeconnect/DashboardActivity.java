package com.example.ieeeconnect;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.ieeeconnect.databinding.ActivityDashboardBinding;
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
        navView.setOnTabSelectedListener(index -> {
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
                    switchFragment(new ProfileFragment());
                    break;
            }
        });

        // Default selection -> Home (also update nav visual state)
        navView.selectTab(0);
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragment_container, fragment);
        tx.commit();
    }
}
