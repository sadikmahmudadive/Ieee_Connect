package com.example.ieeeconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ieeeconnect.R;
import com.example.ieeeconnect.databinding.ActivityChangePasswordBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.changePasswordButton.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "You must be signed in to change your password.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentPassword = "";
        if (binding.currentPasswordInput != null) {
            currentPassword = binding.currentPasswordInput.getText() != null
                    ? binding.currentPasswordInput.getText().toString() : "";
        }

        String newPassword = binding.newPasswordInput.getText() != null
                ? binding.newPasswordInput.getText().toString() : "";

        String confirmPassword = "";
        if (binding.confirmPasswordInput != null) {
            confirmPassword = binding.confirmPasswordInput.getText() != null
                    ? binding.confirmPasswordInput.getText().toString() : "";
        }

        // Validation
        if (TextUtils.isEmpty(currentPassword)) {
            if (binding.currentPasswordLayout != null) {
                binding.currentPasswordLayout.setError("Current password is required");
            }
            return;
        }
        if (binding.currentPasswordLayout != null) binding.currentPasswordLayout.setError(null);

        if (TextUtils.isEmpty(newPassword)) {
            binding.newPasswordLayout.setError("New password is required");
            return;
        }
        if (newPassword.length() < 6) {
            binding.newPasswordLayout.setError("Password must be at least 6 characters");
            return;
        }
        binding.newPasswordLayout.setError(null);

        if (!TextUtils.isEmpty(confirmPassword) && !newPassword.equals(confirmPassword)) {
            if (binding.confirmPasswordLayout != null) {
                binding.confirmPasswordLayout.setError("Passwords do not match");
            }
            return;
        }
        if (binding.confirmPasswordLayout != null) binding.confirmPasswordLayout.setError(null);

        // Disable button and show loading
        binding.changePasswordButton.setEnabled(false);
        binding.changePasswordButton.setText("Updating...");

        // Re-authenticate first
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        String finalNewPassword = newPassword;
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Now update the password
                    user.updatePassword(finalNewPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, PasswordChangedActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                binding.changePasswordButton.setEnabled(true);
                                binding.changePasswordButton.setText("Change Password");
                                binding.newPasswordLayout.setError("Failed: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    binding.changePasswordButton.setEnabled(true);
                    binding.changePasswordButton.setText("Change Password");
                    if (binding.currentPasswordLayout != null) {
                        binding.currentPasswordLayout.setError("Current password is incorrect");
                    } else {
                        Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
