package com.example.ieeeconnect.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.ieeeconnect.R;
import com.example.ieeeconnect.databinding.FragmentSetPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SetPasswordFragment extends Fragment {

    private FragmentSetPasswordBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSetPasswordBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        binding.finishButton.setOnClickListener(v -> {
            String password = binding.newPasswordInput.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordInput.getText().toString().trim();

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = getArguments().getString("email");
            String firstName = getArguments().getString("firstName");
            String lastName = getArguments().getString("lastName");
            String profileImageUrl = getArguments().getString("profileImageUrl");

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
                                    Navigation.findNavController(v).navigate(R.id.action_setPasswordFragment_to_accountReadyFragment);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Sign up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        binding.backButton.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        return binding.getRoot();
    }
}
