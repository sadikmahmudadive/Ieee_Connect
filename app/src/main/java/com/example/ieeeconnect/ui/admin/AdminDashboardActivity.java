package com.example.ieeeconnect.ui.admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import com.example.ieeeconnect.R;

public class AdminDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        boolean isAdmin = getIntent().getBooleanExtra("isAdmin", false);
        String role = getIntent().getStringExtra("role");

        if (savedInstanceState == null) {
            AdminDashboardFragment fragment = new AdminDashboardFragment();
            Bundle args = new Bundle();
            args.putBoolean("isAdmin", isAdmin);
            args.putString("role", role);
            fragment.setArguments(args);
            FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            tx.replace(R.id.fragment_container, fragment);
            tx.commit();
        }
    }
}
