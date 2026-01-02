package com.example.ieeeconnect.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.ieeeconnect.data.remote.CloudinaryManager;
import com.example.ieeeconnect.databinding.ActivitySignUpBinding;

import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private String profileImageUrl;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
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
                    if (extras != null && extras.get("data") != null) {
                        Uri imageUri = (Uri) extras.get("data");
                        binding.profileImage.setImageURI(imageUri);
                        uploadToCloudinary(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.profileImage.setOnClickListener(v -> showImageSourceDialog());

        binding.nextButton.setOnClickListener(v -> {
            String email = binding.emailInput.getText().toString().trim();
            String firstName = binding.firstNameInput.getText().toString().trim();
            String lastName = binding.lastNameInput.getText().toString().trim();

            if (email.isEmpty()) {
                binding.emailLayout.setError("Email is required");
                return;
            }
            if (firstName.isEmpty()) {
                binding.firstNameLayout.setError("First name is required");
                return;
            }
            if (lastName.isEmpty() && !binding.noLastNameCheckbox.isChecked()) {
                binding.lastNameLayout.setError("Last name is required");
                return;
            }

            Intent intent = new Intent(this, SetPasswordActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("firstName", firstName);
            intent.putExtra("lastName", lastName);
            intent.putExtra("profileImageUrl", profileImageUrl);
            startActivity(intent);
        });

        binding.loginText.setOnClickListener(v -> finish());
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload Profile Image");
        builder.setItems(new CharSequence[]{"Upload with Camera", "Upload from Gallery", "Cancel"}, (dialog, which) -> {
            switch (which) {
                case 0: // Camera
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraLauncher.launch(cameraIntent);
                    break;
                case 1: // Gallery
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                            openGallery();
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            openGallery();
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                    }
                    break;
                case 2: // Cancel
                    dialog.dismiss();
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
        CloudinaryManager.upload(imageUri.toString(), new UploadCallback() {
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
                Toast.makeText(SignUpActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                // Reschedule
            }
        });
    }
}
