package com.example.ieeeconnect.activities;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

import com.bumptech.glide.Glide;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.ieeeconnect.data.remote.CloudinaryManager;
import com.example.ieeeconnect.databinding.ActivityEditEventBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditEventActivity extends AppCompatActivity {

    private ActivityEditEventBinding binding;
    private String bannerUrl;
    private String eventId;
    private long selectedStartTime = 0;
    private long selectedEndTime = 0;
    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private boolean isUploadingNewBanner = false;

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
        binding = ActivityEditEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get event data from intent
        eventId = getIntent().getStringExtra("eventId");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String location = getIntent().getStringExtra("location");
        bannerUrl = getIntent().getStringExtra("bannerUrl");
        selectedStartTime = getIntent().getLongExtra("startTime", 0);
        selectedEndTime = getIntent().getLongExtra("endTime", 0);

        if (eventId == null) {
            Toast.makeText(this, "Invalid event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Populate fields with existing data
        binding.eventTitleInput.setText(title);
        binding.eventDescriptionInput.setText(description);
        binding.eventLocationInput.setText(location);

        if (bannerUrl != null && !bannerUrl.isEmpty()) {
            Glide.with(this).load(bannerUrl).into(binding.eventBanner);
        }

        if (selectedStartTime > 0) {
            startCalendar.setTimeInMillis(selectedStartTime);
            updateStartDateTimeDisplay();
        }

        if (selectedEndTime > 0) {
            endCalendar.setTimeInMillis(selectedEndTime);
            updateEndDateTimeDisplay();
        }

        // Start date picker
        binding.startDateInput.setOnClickListener(v -> showStartDatePicker());

        // Start time picker
        binding.startTimeInput.setOnClickListener(v -> showStartTimePicker());

        // End date picker
        binding.endDateInput.setOnClickListener(v -> showEndDatePicker());

        // End time picker
        binding.endTimeInput.setOnClickListener(v -> showEndTimePicker());

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

        binding.updateEventButton.setOnClickListener(v -> updateEvent());
    }

    private void showStartDatePicker() {
        int year = startCalendar.get(Calendar.YEAR);
        int month = startCalendar.get(Calendar.MONTH);
        int day = startCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            startCalendar.set(Calendar.YEAR, selectedYear);
            startCalendar.set(Calendar.MONTH, selectedMonth);
            startCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            selectedStartTime = startCalendar.getTimeInMillis();
            updateStartDateTimeDisplay();
        }, year, month, day);
        datePickerDialog.show();
    }

    private void showStartTimePicker() {
        int hour = startCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = startCalendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            startCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            startCalendar.set(Calendar.MINUTE, selectedMinute);
            selectedStartTime = startCalendar.getTimeInMillis();
            updateStartDateTimeDisplay();
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private void showEndDatePicker() {
        int year = endCalendar.get(Calendar.YEAR);
        int month = endCalendar.get(Calendar.MONTH);
        int day = endCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            endCalendar.set(Calendar.YEAR, selectedYear);
            endCalendar.set(Calendar.MONTH, selectedMonth);
            endCalendar.set(Calendar.DAY_OF_MONTH, selectedDay);
            selectedEndTime = endCalendar.getTimeInMillis();
            updateEndDateTimeDisplay();
        }, year, month, day);
        datePickerDialog.show();
    }

    private void showEndTimePicker() {
        int hour = endCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = endCalendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            endCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            endCalendar.set(Calendar.MINUTE, selectedMinute);
            selectedEndTime = endCalendar.getTimeInMillis();
            updateEndDateTimeDisplay();
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private void updateStartDateTimeDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        binding.startDateInput.setText(dateFormat.format(startCalendar.getTime()));
        binding.startTimeInput.setText(timeFormat.format(startCalendar.getTime()));
    }

    private void updateEndDateTimeDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        binding.endDateInput.setText(dateFormat.format(endCalendar.getTime()));
        binding.endTimeInput.setText(timeFormat.format(endCalendar.getTime()));
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void uploadToCloudinary(Uri imageUri) {
        isUploadingNewBanner = true;
        binding.uploadBannerButton.setEnabled(false);
        binding.uploadBannerButton.setText("Uploading...");

        CloudinaryManager.upload(imageUri, new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                // Upload started
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                // Track progress if needed
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                bannerUrl = (String) resultData.get("secure_url");
                if (bannerUrl == null || bannerUrl.isEmpty()) {
                    bannerUrl = (String) resultData.get("url");
                }
                runOnUiThread(() -> {
                    Toast.makeText(EditEventActivity.this, "Banner uploaded successfully", Toast.LENGTH_SHORT).show();
                    binding.uploadBannerButton.setEnabled(true);
                    binding.uploadBannerButton.setText("Change Banner");
                    isUploadingNewBanner = false;
                });
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                runOnUiThread(() -> {
                    Toast.makeText(EditEventActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    binding.uploadBannerButton.setEnabled(true);
                    binding.uploadBannerButton.setText("Upload Banner");
                    isUploadingNewBanner = false;
                });
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                // Handle reschedule if needed
            }
        });
    }

    private void updateEvent() {
        String title = binding.eventTitleInput.getText() != null ? binding.eventTitleInput.getText().toString().trim() : "";
        String description = binding.eventDescriptionInput.getText() != null ? binding.eventDescriptionInput.getText().toString().trim() : "";
        String location = binding.eventLocationInput.getText() != null ? binding.eventLocationInput.getText().toString().trim() : "";

        if (title.isEmpty() || description.isEmpty() || bannerUrl == null || selectedStartTime == 0 || selectedEndTime == 0) {
            Toast.makeText(this, "Please fill all fields and select date/time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedEndTime < selectedStartTime) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isUploadingNewBanner) {
            Toast.makeText(this, "Please wait for banner upload to complete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update event in Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("locationName", location.isEmpty() ? null : location);
        updates.put("location", location.isEmpty() ? null : location);
        updates.put("bannerUrl", bannerUrl);
        updates.put("startTime", selectedStartTime);
        updates.put("endTime", selectedEndTime);
        updates.put("eventTime", selectedStartTime); // For backward compatibility

        // Update the document directly using the document ID (eventId)
        FirebaseFirestore.getInstance().collection("events")
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Notify parent to reload
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

