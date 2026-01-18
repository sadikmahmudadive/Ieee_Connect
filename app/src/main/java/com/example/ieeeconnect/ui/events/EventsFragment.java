package com.example.ieeeconnect.ui.events;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ieeeconnect.activities.CreateEventActivity;
import com.example.ieeeconnect.databinding.FragmentEventsBinding;
import com.example.ieeeconnect.viewmodels.EventsViewModel;

import java.util.ArrayList;

public class EventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventsAdapter adapter;
    private EventsViewModel viewModel;

    private EventsAdapter.OnRsvpActionListener rsvpListener = (event, rsvpStatus, position) -> {
        // RSVP logic handled in adapter/viewmodel
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new EventsAdapter(rsvpListener);
        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recycler.setAdapter(adapter);

        // USE ACTIVITY SCOPE to stay in sync with HomeFragment
        viewModel = new ViewModelProvider(requireActivity()).get(EventsViewModel.class);
        
        // Observe events and handle empty states explicitly
        viewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            if (events == null || events.isEmpty()) {
                binding.recycler.setVisibility(View.GONE);
                if (binding.emptyStateText != null) binding.emptyStateText.setVisibility(View.VISIBLE);
                adapter.submitList(new ArrayList<>()); // Force clear adapter
            } else {
                binding.recycler.setVisibility(View.VISIBLE);
                if (binding.emptyStateText != null) binding.emptyStateText.setVisibility(View.GONE);
                adapter.submitList(new ArrayList<>(events));
            }
        });

        binding.fabCreateEvent.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateEventActivity.class));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshFromNetwork();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
