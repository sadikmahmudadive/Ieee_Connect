package com.example.ieeeconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ieeeconnect.R;
import com.example.ieeeconnect.databinding.ActivityPasswordChangedBinding;
import com.example.ieeeconnect.MainActivity;

public class PasswordChangedActivity extends AppCompatActivity {

    private ActivityPasswordChangedBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPasswordChangedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
