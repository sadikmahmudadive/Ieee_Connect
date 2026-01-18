package com.example.ieeeconnect.ui.profile;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.ui.events.EventDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class MyEventsAdapter extends RecyclerView.Adapter<MyEventsAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView banner;
        TextView title;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            banner = itemView.findViewById(R.id.event_card_image);
            if (banner == null) banner = itemView.findViewWithTag("event_image"); // Fallback if ID is different
            title = itemView.findViewById(R.id.event_card_title);
            if (title == null) title = (TextView) ((ViewGroup)itemView).getChildAt(1); // Fallback
        }

        void bind(Event event) {
            if (title != null) title.setText(event.getTitle());
            if (banner != null) {
                Glide.with(itemView.getContext())
                        .load(event.getBannerUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(banner);
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), EventDetailActivity.class);
                intent.putExtra("eventId", event.getEventId());
                itemView.getContext().startActivity(intent);
            });
        }
    }
}
