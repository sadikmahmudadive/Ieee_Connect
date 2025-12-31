package com.example.ieeeconnect.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.ieeeconnect.MainActivity;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.databinding.ActivityOnboardingBinding;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        markOnboardingSeen();

        // Static content to mirror provided mock
        binding.title.setText("Welcome to IEEE Organizer");
        binding.subtitle.setText("Organize events, sharing file and Message with other members more easily.");

        LottieAnimationView lottie = binding.illustration;
        lottie.setAnimation(R.raw.onboarding);
        lottie.playAnimation();

        binding.btnLogIn.setOnClickListener(v -> goToAuth());
    }

    private void goToAuth() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void markOnboardingSeen() {
        getSharedPreferences("ieee_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_seen", true)
                .apply();
    }
}
