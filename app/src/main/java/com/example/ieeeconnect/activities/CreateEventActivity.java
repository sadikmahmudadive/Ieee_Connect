package com.example.ieeeconnect.activities;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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
import com.example.ieeeconnect.util.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {

    private ActivityCreateEventBinding binding;
    private String bannerUrl;
    private long selectedEventTime = 0;
    private final Calendar eventCalendar = Calendar.getInstance();

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

        // Date picker
        binding.eventDateInput.setOnClickListener(v -> showDatePicker());

        // Time picker
        binding.eventTimeInput.setOnClickListener(v -> showTimePicker());

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
            String title = binding.eventTitleInput.getText() != null ? binding.eventTitleInput.getText().toString().trim() : "";
            String description = binding.eventDescriptionInput.getText() != null ? binding.eventDescriptionInput.getText().toString().trim() : "";
            String location = binding.eventLocationInput.getText() != null ? binding.eventLocationInput.getText().toString().trim() : "";

            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (title.isEmpty() || description.isEmpty() || bannerUrl == null || selectedEventTime == 0) {
                Toast.makeText(this, "Please fill all fields, upload a banner, and select date/time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use no-arg constructor + setters to avoid constructor mismatch across versions
            Event event = new Event();
            event.setEventId(UUID.randomUUID().toString());
            event.setTitle(title);
            event.setDescription(description);
            event.setEventTime(selectedEventTime);
            event.setBannerUrl(bannerUrl);
            event.setCreatedByUserId(userId);
            event.setGoingUserIds(new ArrayList<>());
            event.setInterestedUserIds(new ArrayList<>());
            event.setLocationName(location.isEmpty() ? null : location);
            event.setStartTime(selectedEventTime);
            event.setEndTime(selectedEventTime);
            event.setId(null); // Firestore will generate
            event.setLocation(location.isEmpty() ? null : location);
            event.setCreatedAt(System.currentTimeMillis());
            event.setCategory(null);

            FirebaseFirestore.getInstance().collection("events")
                    .add(event)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Event created successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK); // Notify parent to reload
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void showDatePicker() {
        int year = eventCalendar.get(Calendar.YEAR);
        int month = eventCalendar.get(Calendar.MONTH);
        int day = eventCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            eventCalendar.set(Calendar.YEAR, selectedYear);
            eventCalendar.set(Calendar.MONTH, selectedMonth);
            eventCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            updateDateDisplay();
        }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        int hour = eventCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = eventCalendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            eventCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            eventCalendar.set(Calendar.MINUTE, selectedMinute);
            updateTimeDisplay();
            selectedEventTime = eventCalendar.getTimeInMillis();
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        binding.eventDateInput.setText(sdf.format(eventCalendar.getTime()));
    }

    private void updateTimeDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        binding.eventTimeInput.setText(sdf.format(eventCalendar.getTime()));
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void uploadToCloudinary(Uri imageUri) {
        // Resize locally first
        try {
            Bitmap original = ImageUtils.getBitmapFromUri(this, imageUri);
            if (original == null) return;
            Bitmap resized = ImageUtils.scaleDownBitmap(original, 1024);
            Uri uploadUri = ImageUtils.writeBitmapToCacheAndGetUri(this, resized, "event_banner_upload.jpg");
            if (uploadUri == null) {
                Toast.makeText(this, "Failed to prepare image for upload", Toast.LENGTH_SHORT).show();
                return;
            }

            // show progress overlay
            binding.bannerProgressContainer.setVisibility(View.VISIBLE);
            binding.bannerProgressBar.setProgress(0);
            binding.bannerProgressText.setText("0%");
            binding.uploadBannerButton.setEnabled(false);

            CloudinaryManager.upload(uploadUri, new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    // started
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {
                    try {
                        int percent = (int) ((bytes * 100) / (totalBytes == 0 ? 1 : totalBytes));
                        runOnUiThread(() -> {
                            binding.bannerProgressBar.setIndeterminate(false);
                            binding.bannerProgressBar.setProgress(percent);
                            binding.bannerProgressText.setText(percent + "%");
                        });
                    } catch (Exception ignored) {}
                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    Object urlObj = resultData != null ? resultData.get("secure_url") : null;
                    final String url = urlObj != null ? urlObj.toString() : (resultData != null ? String.valueOf(resultData.get("url")) : null);
                    bannerUrl = url;
                    runOnUiThread(() -> {
                        if (url != null) {
                            binding.eventBanner.setImageURI(null);
                            com.bumptech.glide.Glide.with(CreateEventActivity.this).load(url).into(binding.eventBanner);
                        }
                        binding.bannerProgressContainer.setVisibility(View.GONE);
                        binding.uploadBannerButton.setEnabled(true);
                    });

                    // cleanup temp file
                    try {
                        File f = new File(uploadUri.getPath());
                        if (f.exists()) f.delete();
                    } catch (Exception ignored) {}
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreateEventActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        binding.bannerProgressContainer.setVisibility(View.GONE);
                        binding.uploadBannerButton.setEnabled(true);
                    });
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {
                    // handle retry
                }
            });

        } catch (IOException e) {
            Toast.makeText(this, "Failed to prepare image for upload", Toast.LENGTH_SHORT).show();
        }
    }
}
