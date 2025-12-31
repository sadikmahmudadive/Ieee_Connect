package com.example.ieeeconnect.ui.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ieeeconnect.data.repository.EventsRepository;
import com.example.ieeeconnect.databinding.FragmentEventsBinding;
import com.example.ieeeconnect.domain.model.Event;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventsAdapter adapter;
    private EventsRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new EventsAdapter(new ArrayList<>());
        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recycler.setAdapter(adapter);

        repository = new EventsRepository();
        repository.loadEvents(events -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> adapter.setItems(events));
        });
    }
}

