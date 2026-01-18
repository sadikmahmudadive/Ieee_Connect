package com.example.ieeeconnect.ui.events;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.viewmodels.EventsViewModel;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class EventDetailActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "eventId";
    private static final String TAG = "EventDetailActivity";

    private ImageView bannerImage;
    private TextView titleText;
    private TextView descriptionText;
    private TextView timeText;
    private TextView locationText;
    private MaterialButton goingButton;
    private MaterialButton interestedButton;
    private MaterialButton notGoingButton;
    private MaterialButton addToCalendarButton;
    private MaterialButton deleteEventButton;
    private MaterialButton editEventButton;
    private CollapsingToolbarLayout collapsingToolbar;
    
    private EventsViewModel viewModel;
    private Event currentEvent;
    private String eventId;

    public static void startWithTransition(AppCompatActivity activity, View sourceImage, String eventId) {
        Intent intent = new Intent(activity, EventDetailActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                activity,
                sourceImage,
                "eventBannerTransition"
        );
        activity.startActivity(intent, options.toBundle());
    }

    private final ActivityResultLauncher<Intent> editEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (ActivityResult result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadEventDetails();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        initViews();
        viewModel = new ViewModelProvider(this).get(EventsViewModel.class);

        handleIntent(getIntent());

        if (eventId != null) {
            loadEventDetails();
            observeEventChanges();
        } else {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
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
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        // Fallback for older intent structures if any
        if (eventId == null) {
            eventId = intent.getStringExtra("event");
        }
    }

    private void loadEventDetails() {
        if (eventId == null) return;
        
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentEvent = doc.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setEventId(doc.getId());
                            displayEventDetails();
                        }
                    } else {
                        Toast.makeText(this, "Event no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading event", e));
    }

    private void observeEventChanges() {
        viewModel.getAllEvents().observe(this, events -> {
            if (events == null) return;
            for (Event e : events) {
                if (e.getEventId().equals(eventId)) {
                    currentEvent = e;
                    updateRsvpButtons();
                    break;
                }
            }
        });
    }

    private void displayEventDetails() {
        if (currentEvent == null) return;

        titleText.setText(currentEvent.getTitle());
        descriptionText.setText(currentEvent.getDescription());
        timeText.setText(formatEventTime(currentEvent.getStartTime(), currentEvent.getEndTime()));
        
        if (!TextUtils.isEmpty(currentEvent.getLocationName())) {
            locationText.setText(currentEvent.getLocationName());
            locationText.setVisibility(View.VISIBLE);
        } else {
            locationText.setVisibility(View.GONE);
        }

        if (collapsingToolbar != null) collapsingToolbar.setTitle(currentEvent.getTitle());
        
        Glide.with(this)
                .load(currentEvent.getBannerUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(bannerImage);

        setupClickListeners();
        updateRsvpButtons();
        checkAdminPermissions();
    }

    private void setupClickListeners() {
        String userId = FirebaseAuth.getInstance().getUid();
        
        goingButton.setOnClickListener(v -> {
            if (userId != null) viewModel.toggleGoing(eventId, userId);
        });

        interestedButton.setOnClickListener(v -> {
            if (userId != null) viewModel.toggleInterested(eventId, userId);
        });

        notGoingButton.setOnClickListener(v -> {
            Toast.makeText(this, "Selection saved", Toast.LENGTH_SHORT).show();
        });

        addToCalendarButton.setOnClickListener(v -> addToCalendar());
        editEventButton.setOnClickListener(v -> editEvent());
        deleteEventButton.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void updateRsvpButtons() {
        if (currentEvent == null) return;
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        boolean isGoing = currentEvent.getGoingUserIds().contains(userId);
        boolean isInterested = currentEvent.getInterestedUserIds().contains(userId);

        // Visual feedback for selection
        goingButton.setAlpha(isGoing ? 1.0f : 0.6f);
        goingButton.setStrokeWidth(isGoing ? 4 : 0);

        interestedButton.setAlpha(isInterested ? 1.0f : 0.6f);
        interestedButton.setStrokeWidth(isInterested ? 4 : 0);
    }

    private void checkAdminPermissions() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    
                    String role = doc.getString("role");
                    boolean isAdmin = Boolean.TRUE.equals(doc.getBoolean("isAdmin"));
                    boolean isPrivileged = isAdmin || "ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "EXCOM".equals(role);
                    boolean isCreator = uid.equals(currentEvent.getCreatedByUserId());

                    if (isPrivileged || isCreator) {
                        editEventButton.setVisibility(View.VISIBLE);
                        deleteEventButton.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void addToCalendar() {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, currentEvent.getTitle())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, currentEvent.getStartTime())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, currentEvent.getEndTime())
                .putExtra(CalendarContract.Events.EVENT_LOCATION, currentEvent.getLocationName())
                .putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        startActivity(intent);
    }

    private void editEvent() {
        Intent intent = new Intent(this, com.example.ieeeconnect.activities.EditEventActivity.class);
        intent.putExtra("eventId", eventId);
        editEventLauncher.launch(intent);
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent() {
        FirebaseFirestore.getInstance().collection("events").document(eventId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private String formatEventTime(long start, long end) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        return sdf.format(new Date(start));
    }
}
