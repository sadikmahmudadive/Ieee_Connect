package com.example.ieeeconnect.ui.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.ui.events.EventDetailActivity;

import java.util.List;

/**
 * EventsAdapter for displaying events in a RecyclerView.
 * <p>
 * Admin Dashboard Fragment for IEEE BUBT
 * Only visible to users with isAdmin or role=SUPER_ADMIN/ADMIN/EXCOM
 * Features: Quick Stats, Recent Activity, Quick Actions Grid, FAB for Broadcast
 * Navigation to: MemberListActivity, QrScannerActivity, Event Management, Content Moderation
 * TODO: Implement fragment_admin_dashboard.xml layout with header, grid, and FAB
 * TODO: Implement AdminDashboardFragment.java with navigation logic
 * TODO: Add role-based access control in main navigation
 * TODO: Scaffold MemberListActivity.java and QrScannerActivity.java
 * TODO: Add edit/delete event support for admins in EventsAdapter/EventDetail
 * TODO: Implement push notification broadcaster UI
 * TODO: Implement moderation queue for reported content
 * TODO: Ensure initial superadmin and admin users exist in Firestore
 */

public class EventsAdapter extends ListAdapter<Event, EventsAdapter.EventVH> {

    public interface OnRsvpActionListener {
        void onRsvp(Event event, String rsvpStatus, int position);
    }

    private final OnRsvpActionListener rsvpListener;

    public EventsAdapter(OnRsvpActionListener rsvpListener) {
        super(DIFF_CALLBACK);
        this.rsvpListener = rsvpListener;
    }

    public void updateEvent(Event event, int position) {
        // For optimistic update: create a new list and submit
        List<Event> current = new java.util.ArrayList<>(getCurrentList());
        current.set(position, event);
        submitList(current);
    }

    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH holder, int position) {
        Event event = getItem(position);
        holder.title.setText(event.getTitle());
        holder.description.setText(event.getDescription());
        Glide.with(holder.banner.getContext())
                .load(event.getBannerUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.banner);
        holder.itemView.setOnClickListener(v -> {
            EventDetailActivity.startWithTransition(
                    (AppCompatActivity) v.getContext(),
                    holder.banner,
                    event.getEventId()
            );
        });
        // RSVP button logic (optimistic UI)
        holder.btnGoing.setOnClickListener(v -> rsvpListener.onRsvp(event, "GOING", position));
        holder.btnInterested.setOnClickListener(v -> rsvpListener.onRsvp(event, "INTERESTED", position));
        holder.btnNotGoing.setOnClickListener(v -> rsvpListener.onRsvp(event, "NOT_GOING", position));
        // Optionally, update button states based on event's RSVP status
        // (Assume event has a getRsvpStatus() method, or similar)
        // Example:
        // String status = event.getRsvpStatus();
        // holder.btnGoing.setEnabled(!"GOING".equals(status));
        // ...
    }

    public static class EventVH extends RecyclerView.ViewHolder {
        ImageView banner;
        TextView title;
        TextView description;
        Button btnGoing, btnInterested, btnNotGoing;
        EventVH(@NonNull View itemView) {
            super(itemView);
            banner = itemView.findViewById(R.id.event_banner);
            title = itemView.findViewById(R.id.event_title);
            description = itemView.findViewById(R.id.event_description);
            btnGoing = itemView.findViewById(R.id.btn_going);
            btnInterested = itemView.findViewById(R.id.btn_interested);
            btnNotGoing = itemView.findViewById(R.id.btn_not_going);
        }
    }

    public static final DiffUtil.ItemCallback<Event> DIFF_CALLBACK = new DiffUtil.ItemCallback<Event>() {
        @Override
        public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return oldItem.getEventId().equals(newItem.getEventId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
            return oldItem.equals(newItem);
        }
    };
}
