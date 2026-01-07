package com.example.ieeeconnect.ui.events;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.Event;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.TimeZone;

public class EventDetailActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT = "event";

    private ImageView bannerImage;
    private TextView titleText;
    private TextView descriptionText;
    private TextView timeText;
    private TextView locationText;
    private Button goingButton;
    private Button interestedButton;
    private Button notGoingButton;
    private Button addToCalendarButton;
    private MaterialButton deleteEventButton;
    private MaterialButton editEventButton;
    private CollapsingToolbarLayout collapsingToolbar;
    private Event currentEvent;
    private String eventId;

    private final ActivityResultLauncher<Intent> editEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (ActivityResult result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Reload event details when returning from edit
                    if (eventId != null) {
                        loadEventDetails();
                        Toast.makeText(this, "Event updated, reloading...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        bannerImage = findViewById(R.id.bannerImage);
        titleText = findViewById(R.id.titleText);
        descriptionText = findViewById(R.id.descriptionText);
        timeText = findViewById(R.id.timeText);
        locationText = findViewById(R.id.locationText);
        goingButton = findViewById(R.id.goingButton);
        interestedButton = findViewById(R.id.interestedButton);
        notGoingButton = findViewById(R.id.notGoingButton);
        addToCalendarButton = findViewById(R.id.addToCalendarButton);
        deleteEventButton = findViewById(R.id.deleteEventButton);
        editEventButton = findViewById(R.id.editEventButton);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);

        // Get event ID from intent and fetch event details from Firestore
        eventId = getIntent().getStringExtra(EXTRA_EVENT);
        if (eventId == null) {
            finish();
            return;
        }

        loadEventDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Auto-reload event details when activity is resumed
        if (eventId != null) {
            loadEventDetails();
        }
    }

    private void loadEventDetails() {
        // Show loading state
        collapsingToolbar.setTitle("Loading...");

        // Fetch event from Firestore
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentEvent = documentSnapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setEventId(documentSnapshot.getId());
                            displayEventDetails();
                        } else {
                            Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayEventDetails() {
        titleText.setText(currentEvent.getTitle());
        descriptionText.setText(currentEvent.getDescription());
        timeText.setText(formatEventTime(currentEvent.getStartTime(), currentEvent.getEndTime()));

        // Display location if available, otherwise hide the TextView
        String location = currentEvent.getLocationName();
        if (location != null && !location.trim().isEmpty()) {
            locationText.setText(location);
            locationText.setVisibility(View.VISIBLE);
        } else {
            locationText.setVisibility(View.GONE);
        }

        collapsingToolbar.setTitle(currentEvent.getTitle());
        Glide.with(this).load(currentEvent.getBannerUrl()).into(bannerImage);

        addToCalendarButton.setOnClickListener(v -> {
            if (currentEvent == null) return;
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, currentEvent.getTitle())
                    .putExtra(CalendarContract.Events.DESCRIPTION, currentEvent.getDescription())
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, currentEvent.getLocationName() != null ? currentEvent.getLocationName() : "")
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, currentEvent.getStartTime())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, currentEvent.getEndTime())
                    .putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            startActivity(intent);
        });

        // Check if user is admin and show delete button
        checkAdminStatusAndShowDeleteButton();

        deleteEventButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        editEventButton.setOnClickListener(v -> editEvent());
    }

    private void checkAdminStatusAndShowDeleteButton() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            deleteEventButton.setVisibility(View.GONE);
            editEventButton.setVisibility(View.GONE);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    boolean isAdmin = false;
                    String role = "";
                    if (doc.exists()) {
                        Object isAdminObj = doc.get("isAdmin");
                        Object roleObj = doc.get("role");
                        if (isAdminObj instanceof Boolean) {
                            isAdmin = (Boolean) isAdminObj;
                        } else if (isAdminObj != null) {
                            String val = isAdminObj.toString().trim().toLowerCase();
                            isAdmin = val.equals("true") || val.equals("1") || val.equals("yes");
                        }
                        if (roleObj != null) role = roleObj.toString().trim();
                    }

                    // Show delete and edit buttons for admins or event creators
                    boolean isEventCreator = currentEvent.getCreatedByUserId() != null
                            && currentEvent.getCreatedByUserId().equals(uid);
                    boolean isPrivileged = isAdmin || "SUPER_ADMIN".equalsIgnoreCase(role)
                            || "ADMIN".equalsIgnoreCase(role) || "EXCOM".equalsIgnoreCase(role);

                    if (isPrivileged || isEventCreator) {
                        deleteEventButton.setVisibility(View.VISIBLE);
                        editEventButton.setVisibility(View.VISIBLE);
                    } else {
                        deleteEventButton.setVisibility(View.GONE);
                        editEventButton.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    deleteEventButton.setVisibility(View.GONE);
                    editEventButton.setVisibility(View.GONE);
                });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_delete)
                .show();
    }

    private void deleteEvent() {
        if (currentEvent == null || currentEvent.getEventId() == null) {
            Toast.makeText(this, "Cannot delete event: Invalid event", Toast.LENGTH_SHORT).show();
            return;
        }

        // Try to find and delete by eventId field in Firestore
        FirebaseFirestore.getInstance().collection("events")
                .whereEqualTo("eventId", currentEvent.getEventId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Delete the first matching document
                        querySnapshot.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK); // Notify parent to reload
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to delete event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void editEvent() {
        if (currentEvent == null) {
            Toast.makeText(this, "Cannot edit event: Invalid event", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start EditEventActivity with the event data using launcher
        Intent intent = new Intent(this, com.example.ieeeconnect.activities.EditEventActivity.class);
        intent.putExtra("eventId", currentEvent.getEventId());
        intent.putExtra("title", currentEvent.getTitle());
        intent.putExtra("description", currentEvent.getDescription());
        intent.putExtra("location", currentEvent.getLocationName());
        intent.putExtra("bannerUrl", currentEvent.getBannerUrl());
        intent.putExtra("startTime", currentEvent.getStartTime());
        intent.putExtra("endTime", currentEvent.getEndTime());
        editEventLauncher.launch(intent);
    }

    public static void startWithTransition(AppCompatActivity activity, View sharedImage, String eventId) {
        Intent intent = new Intent(activity, EventDetailActivity.class);
        intent.putExtra(EXTRA_EVENT, eventId);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                Pair.create(sharedImage, "eventBannerTransition")
        );
        activity.startActivity(intent, options.toBundle());
    }

    private String formatEventTime(long start, long end) {
        // Simple formatting, you can improve as needed
        java.text.DateFormat df = android.text.format.DateFormat.getMediumDateFormat(this);
        java.text.DateFormat tf = android.text.format.DateFormat.getTimeFormat(this);
        String startStr = df.format(new java.util.Date(start)) + " " + tf.format(new java.util.Date(start));
        String endStr = df.format(new java.util.Date(end)) + " " + tf.format(new java.util.Date(end));
        return startStr + " - " + endStr;
    }
}
