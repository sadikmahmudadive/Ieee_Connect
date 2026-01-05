package com.example.ieeeconnect.ui.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.Event;
import com.google.android.material.appbar.CollapsingToolbarLayout;

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
    private CollapsingToolbarLayout collapsingToolbar;

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
        collapsingToolbar = findViewById(R.id.collapsingToolbar);

        Event event = (Event) getIntent().getSerializableExtra(EXTRA_EVENT);
        if (event == null) finish();

        titleText.setText(event.getTitle());
        descriptionText.setText(event.getDescription());
        timeText.setText(formatEventTime(event.getStartTime(), event.getEndTime()));
        locationText.setText(event.getLocationName());
        collapsingToolbar.setTitle(event.getTitle());
        Glide.with(this).load(event.getBannerUrl()).into(bannerImage);

        addToCalendarButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, event.getTitle())
                    .putExtra(CalendarContract.Events.DESCRIPTION, event.getDescription())
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, event.getLocationName())
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getStartTime())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getEndTime())
                    .putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            startActivity(intent);
        });

        // TODO: Implement RSVP logic and map snippet
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
