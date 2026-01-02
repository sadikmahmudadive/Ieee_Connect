package com.example.ieeeconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ieeeconnect.databinding.ActivitySetPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SetPasswordActivity extends AppCompatActivity {

    private ActivitySetPasswordBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        binding.finishButton.setOnClickListener(v -> {
            String password = binding.newPasswordInput.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordInput.getText().toString().trim();

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = getIntent();
            String email = intent.getStringExtra("email");
            String firstName = intent.getStringExtra("firstName");
            String lastName = intent.getStringExtra("lastName");
            String profileImageUrl = intent.getStringExtra("profileImageUrl");

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String userId = authResult.getUser().getUid();
                        Map<String, Object> user = new HashMap<>();
                        user.put("firstName", firstName);
                        user.put("lastName", lastName);
                        user.put("profileImageUrl", profileImageUrl);
                        user.put("email", email);

                        firestore.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    startActivity(new Intent(this, AccountReadyActivity.class));
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Sign up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        binding.backButton.setOnClickListener(v -> finish());
    }
}
