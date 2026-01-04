package com.example.ieeeconnect.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.data.remote.CloudinaryManager;
import com.example.ieeeconnect.databinding.FragmentFillProfileInfoBinding;

import java.util.Map;

public class FillProfileInfoFragment extends Fragment {

    private FragmentFillProfileInfoBinding binding;
    private String profileImageUrl;
    private String email;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(requireContext(), "Permission denied to read external storage", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    binding.profileImage.setImageURI(imageUri);
                    uploadToCloudinary(imageUri);
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Uri imageUri = (Uri) extras.get("data");
                    binding.profileImage.setImageURI(imageUri);
                    uploadToCloudinary(imageUri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFillProfileInfoBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            email = getArguments().getString("email");
            binding.emailText.setText(email);
        }

        binding.profileImage.setOnClickListener(v -> showImageSourceDialog());

        binding.nextButton.setOnClickListener(v -> {
            String firstName = binding.firstNameInput.getText().toString().trim();
            String lastName = binding.lastNameInput.getText().toString().trim();

            if (firstName.isEmpty()) {
                binding.firstNameLayout.setError("First name is required");
                return;
            }
            if (lastName.isEmpty() && !binding.noLastNameCheckbox.isChecked()) {
                binding.lastNameLayout.setError("Last name is required");
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putString("email", email);
            bundle.putString("firstName", firstName);
            bundle.putString("lastName", lastName);
            bundle.putString("profileImageUrl", profileImageUrl);
            Navigation.findNavController(v).navigate(R.id.action_fillProfileInfoFragment_to_setPasswordFragment, bundle);
        });

        binding.loginText.setOnClickListener(v -> requireActivity().finish());

        return binding.getRoot();
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Upload Profile Image");
        builder.setItems(new CharSequence[]{"Upload with Camera", "Upload from Gallery"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(cameraIntent);
                    break;
                case 1:
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        openGallery();
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                    break;
            }
        });
        builder.show();
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void uploadToCloudinary(Uri imageUri) {
        CloudinaryManager.upload(imageUri, new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                // Show progress
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                // Show progress
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                profileImageUrl = (String) resultData.get("url");
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(requireContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                // Reschedule
            }
        });
    }
}
