package com.example.ieeeconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ieeeconnect.R;
import com.example.ieeeconnect.databinding.ActivityChangePasswordBinding;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.changePasswordButton.setOnClickListener(v -> {
            // Add logic to change password here
            Intent intent = new Intent(this, PasswordChangedActivity.class);
            startActivity(intent);
        });
    }
}
