package com.example.ieeeconnect.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ieeeconnect.MainActivity;
import com.example.ieeeconnect.R;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "ieee_prefs";
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        TextView title = root.findViewById(R.id.title);
        TextView subtitle = root.findViewById(R.id.subtitle);
        title.setText("Profile");
        subtitle.setText("Profile and settings");

        Button logout = root.findViewById(R.id.btnLogout);
        logout.setOnClickListener(v -> {
            // Sign out
            FirebaseAuth.getInstance().signOut();
            // Clear onboarding flag so onboarding shows once after logout
            if (getActivity() != null) {
                getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_ONBOARDING_SEEN, false).apply();
                // Navigate back to login (MainActivity)
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        return root;
    }
}
