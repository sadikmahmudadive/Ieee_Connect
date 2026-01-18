package com.example.ieeeconnect.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.activities.CreateEventActivity;
import com.example.ieeeconnect.databinding.FragmentHomeBinding;
import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.ui.events.EventDetailActivity;
import com.example.ieeeconnect.viewmodels.EventsViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private FeedAdapter adapter;
    private FeaturedAdapter featuredAdapter;
    private EventsViewModel viewModel;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private CountDownTimer heroTimer;

    private final ActivityResultLauncher<Intent> createEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (ActivityResult result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (viewModel != null) viewModel.refreshFromNetwork();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup UI
        binding.topAppBar.setTitle("");
        binding.toolbarLogo.setImageResource(R.drawable.ic_ieee_logo);

        adapter = new FeedAdapter();
        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recycler.setAdapter(adapter);

        featuredAdapter = new FeaturedAdapter();
        binding.featuredRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.featuredRecycler.setAdapter(featuredAdapter);

        // USE ACTIVITY SCOPE to prevent stale data reloads on tab switch
        viewModel = new ViewModelProvider(requireActivity()).get(EventsViewModel.class);
        
        viewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            boolean isEmpty = (events == null || events.isEmpty());
            android.util.Log.d("HomeFragment", "Received events: " + (events == null ? "null" : events.size() + " events"));
            if (events != null) {
                for (com.example.ieeeconnect.domain.model.Event e : events) {
                    android.util.Log.d("HomeFragment", "Event: " + e.getTitle() + ", id: " + e.getEventId() + ", start: " + e.getStartTime());
                }
            }

            // Update Feed
            if (isEmpty) {
                binding.recycler.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.VISIBLE);
                adapter.submitList(new ArrayList<>());
            } else {
                binding.recycler.setVisibility(View.VISIBLE);
                binding.emptyState.setVisibility(View.GONE);
                adapter.submitList(new ArrayList<>(events));
            }

            // Update Featured
            if (isEmpty) {
                binding.featuredRecycler.setVisibility(View.GONE);
                featuredAdapter.submitList(new ArrayList<>());
            } else {
                binding.featuredRecycler.setVisibility(View.VISIBLE);
                List<Event> featured = events.size() > 5 ? events.subList(0, 5) : events;
                featuredAdapter.submitList(new ArrayList<>(featured));
            }

            // Update Hero
            long now = System.currentTimeMillis();
            Event nextEvent = isEmpty ? null : events.stream()
                    .filter(event -> event.getStartTime() > now)
                    .min(Comparator.comparingLong(Event::getStartTime))
                    .orElse(null);

            if (nextEvent != null) {
                binding.heroContainer.setVisibility(View.VISIBLE);
                binding.heroTitle.setText(nextEvent.getTitle());
                Glide.with(this).load(nextEvent.getBannerUrl()).placeholder(R.drawable.ic_launcher_background).into(binding.heroBanner);
                
                if (heroTimer != null) heroTimer.cancel();
                long diff = Math.max(0, nextEvent.getStartTime() - now);
                heroTimer = new CountDownTimer(diff, 1000) {
                    @Override public void onTick(long l) { binding.heroCountdown.setText(formatCountdown(l)); }
                    @Override public void onFinish() { binding.heroCountdown.setText(getString(R.string.starting_now)); }
                }.start();

                binding.heroRegister.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                    intent.putExtra("eventId", nextEvent.getEventId());
                    startActivity(intent);
                });
            } else {
                binding.heroContainer.setVisibility(View.GONE);
                if (heroTimer != null) heroTimer.cancel();
            }

            // Global UI state
            binding.swipeRefresh.setRefreshing(false);
            binding.shimmer.stopShimmer();
            binding.shimmer.setVisibility(View.GONE);
            binding.offlineBanner.setVisibility(isNetworkAvailable() ? View.GONE : View.VISIBLE);

            Set<String> categories = new HashSet<>();
            if (events != null) for (Event e : events) if (e.getCategory() != null) categories.add(e.getCategory());
            binding.categoriesTextView.setText(TextUtils.join(", ", categories));
        });

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refreshFromNetwork());
        
        binding.shimmer.setVisibility(View.VISIBLE);
        binding.shimmer.startShimmer();

        checkAdminAndShowFab();
    }

    private String formatCountdown(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format(Locale.getDefault(), "Starts in %02d:%02d:%02d", h, m, s);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.refreshFromNetwork();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (heroTimer != null) heroTimer.cancel();
        binding = null;
    }

    private void checkAdminAndShowFab() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("committee").document(uid).get().addOnSuccessListener(doc -> {
            if (binding == null) return;
            if (doc.exists()) {
                binding.fabAdd.setVisibility(View.VISIBLE);
            } else {
                firestore.collection("roles").document(uid).get().addOnSuccessListener(rdoc -> {
                    if (binding != null) binding.fabAdd.setVisibility((rdoc != null && rdoc.exists() && Boolean.TRUE.equals(rdoc.getBoolean("isAdmin"))) ? View.VISIBLE : View.GONE);
                });
            }
        });
    }

    private boolean isNetworkAvailable() {
        Context ctx = requireContext().getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private static class FeaturedAdapter extends androidx.recyclerview.widget.ListAdapter<Event, FeaturedAdapter.VH> {
        protected FeaturedAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                 @Override public boolean areItemsTheSame(@NonNull Event o, @NonNull Event n) { return o.getEventId().equals(n.getEventId()); }
                 @Override public boolean areContentsTheSame(@NonNull Event o, @NonNull Event n) { return o.equals(n); }
             });
         }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_featured, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) { h.bind(getItem(p)); }
        static class VH extends RecyclerView.ViewHolder {
            android.widget.ImageView banner;
            android.widget.TextView title;
            VH(@NonNull View v) { super(v); banner = v.findViewById(R.id.featured_image); title = v.findViewById(R.id.featured_title); }
            void bind(Event e) { title.setText(e.getTitle()); Glide.with(banner.getContext()).load(e.getBannerUrl()).placeholder(R.drawable.ic_launcher_background).into(banner); }
        }
    }

    private static class FeedAdapter extends androidx.recyclerview.widget.ListAdapter<Event, FeedAdapter.VH> {
        protected FeedAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                @Override public boolean areItemsTheSame(@NonNull Event o, @NonNull Event n) { return o.getEventId().equals(n.getEventId()); }
                @Override public boolean areContentsTheSame(@NonNull Event o, @NonNull Event n) { return o.equals(n); }
            });
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_feed_post, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) { h.bind(getItem(p)); }
        static class VH extends RecyclerView.ViewHolder {
            android.widget.ImageView banner;
            android.widget.TextView title, description, time, likes;
            VH(@NonNull View v) {
                super(v);
                banner = v.findViewById(R.id.post_image); title = v.findViewById(R.id.post_title);
                description = v.findViewById(R.id.post_body); time = v.findViewById(R.id.post_time);
                likes = v.findViewById(R.id.post_likes);
             }
             void bind(Event event) {
                 title.setText(event.getTitle()); description.setText(event.getDescription());
                 time.setText(event.getFormattedTime()); likes.setText(String.valueOf(event.getLikes()));
                 Glide.with(banner.getContext()).load(event.getBannerUrl()).placeholder(R.drawable.ic_launcher_background).into(banner);
             }
         }
     }
 }
