package com.example.ieeeconnect.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.ui.events.EventDetailActivity;
import com.example.ieeeconnect.viewmodels.EventsViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Calendar view showing events. Users can pick a date and see events on that day.
 */
public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView rvCalendarEvents;
    private TextView tvSelectedDate;
    private TextView tvCalendarEmpty;
    private EventsViewModel viewModel;
    private CalendarEventsAdapter adapter;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
    private long selectedDateMillis = System.currentTimeMillis();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarView = view.findViewById(R.id.calendarView);
        rvCalendarEvents = view.findViewById(R.id.rvCalendarEvents);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvCalendarEmpty = view.findViewById(R.id.tvCalendarEmpty);

        adapter = new CalendarEventsAdapter();
        rvCalendarEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCalendarEvents.setAdapter(adapter);

        // Use activity scope to share events data with Home/Events fragments
        viewModel = new ViewModelProvider(requireActivity()).get(EventsViewModel.class);

        // Set initial date label
        updateDateLabel(selectedDateMillis);

        // Calendar date change listener
        calendarView.setOnDateChangeListener((v, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = cal.getTimeInMillis();
            updateDateLabel(selectedDateMillis);
            filterEventsForDate(selectedDateMillis);
        });

        // Observe all events
        viewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            filterEventsForDate(selectedDateMillis);
        });
    }

    private void updateDateLabel(long millis) {
        tvSelectedDate.setText("Events on " + dateFormat.format(new Date(millis)));
    }

    private void filterEventsForDate(long dateMillis) {
        List<Event> allEvents = viewModel.getAllEvents().getValue();
        if (allEvents == null) {
            showEmpty(true);
            return;
        }

        // Get start and end of the selected day
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(dateMillis);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        long dayStart = startCal.getTimeInMillis();

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(dayStart);
        endCal.add(Calendar.DAY_OF_MONTH, 1);
        long dayEnd = endCal.getTimeInMillis();

        List<Event> filtered = new ArrayList<>();
        for (Event event : allEvents) {
            long eventTime = event.getStartTime();
            if (eventTime == 0) eventTime = event.getEventTime();
            if (eventTime >= dayStart && eventTime < dayEnd) {
                filtered.add(event);
            }
        }

        adapter.setEvents(filtered);
        showEmpty(filtered.isEmpty());
    }

    private void showEmpty(boolean empty) {
        tvCalendarEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvCalendarEvents.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── Adapter ─────────────────────────────────────────────────

    private class CalendarEventsAdapter extends RecyclerView.Adapter<CalendarEventsAdapter.VH> {
        private List<Event> events = new ArrayList<>();

        void setEvents(List<Event> list) {
            this.events = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_event, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.bind(events.get(position));
        }

        @Override
        public int getItemCount() { return events.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView banner;
            TextView title, time, location;

            VH(@NonNull View itemView) {
                super(itemView);
                banner = itemView.findViewById(R.id.eventCardBanner);
                title = itemView.findViewById(R.id.eventCardTitle);
                time = itemView.findViewById(R.id.eventCardTime);
                location = itemView.findViewById(R.id.eventCardLocation);
            }

            void bind(Event event) {
                title.setText(event.getTitle());
                time.setText(event.getReadableStartTime());
                if (location != null) {
                    String loc = event.getLocationName();
                    if (loc == null || loc.isEmpty()) loc = event.getLocation();
                    location.setText(loc != null ? loc : "");
                }

                if (event.getBannerUrl() != null && !event.getBannerUrl().isEmpty()) {
                    Glide.with(banner.getContext())
                            .load(event.getBannerUrl())
                            .placeholder(R.drawable.ic_launcher_background)
                            .into(banner);
                }

                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                    intent.putExtra("eventId", event.getEventId());
                    startActivity(intent);
                });
            }
        }
    }
}
