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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.ieeeconnect.data.remote.CloudinaryManager;
import com.example.ieeeconnect.databinding.ActivityCreateEventBinding;
import com.example.ieeeconnect.domain.model.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {

    private ActivityCreateEventBinding binding;
    private String bannerUrl;

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
                    binding.eventBanner.setImageURI(imageUri);
                    uploadToCloudinary(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.uploadBannerButton.setOnClickListener(v -> {
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
        });

        binding.createEventButton.setOnClickListener(v -> {
            String title = binding.eventTitleInput.getText().toString().trim();
            String description = binding.eventDescriptionInput.getText().toString().trim();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (title.isEmpty() || description.isEmpty() || bannerUrl == null) {
                Toast.makeText(this, "Please fill all fields and upload a banner", Toast.LENGTH_SHORT).show();
                return;
            }

            Event event = new Event(UUID.randomUUID().toString(), title, description, System.currentTimeMillis(), bannerUrl, userId, new ArrayList<>(), new ArrayList<>(), null, 0, 0);

            FirebaseFirestore.getInstance().collection("events")
                    .add(event)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Event created successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
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
                bannerUrl = (String) resultData.get("url");
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Toast.makeText(CreateEventActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                // Reschedule
            }
        });
    }
}
