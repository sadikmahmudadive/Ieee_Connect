package com.example.ieeeconnect.ui.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.domain.model.Event;

import java.util.ArrayList;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventVH> {

    private final List<Event> items;

    public EventsAdapter(List<Event> items) {
        this.items = items;
    }

    public void setItems(List<Event> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH holder, int position) {
        Event event = items.get(position);
        holder.title.setText(event.getTitle());
        holder.time.setText(event.getStartTimeIso());
        Glide.with(holder.banner.getContext())
                .load(event.getBannerUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.banner);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EventVH extends RecyclerView.ViewHolder {
        ImageView banner;
        TextView title;
        TextView time;
        Button interested;
        Button going;
        EventVH(@NonNull View itemView) {
            super(itemView);
            banner = itemView.findViewById(R.id.eventBanner);
            title = itemView.findViewById(R.id.eventTitle);
            time = itemView.findViewById(R.id.eventTime);
            interested = itemView.findViewById(R.id.rsvpButton);
            going = itemView.findViewById(R.id.goingButton);
        }
    }
}

