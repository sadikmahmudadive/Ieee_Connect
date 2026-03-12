package com.example.ieeeconnect.ui.home;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.activities.CreateEventActivity;
import com.example.ieeeconnect.databinding.FragmentHomeBinding;
import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.ui.events.EventDetailActivity;
import com.example.ieeeconnect.viewmodels.EventsViewModel;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FeedAdapter feedAdapter;
    private FeaturedAdapter featuredAdapter;
    private EventsViewModel viewModel;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private CountDownTimer heroTimer;
    private List<Event> allEventsCache = new ArrayList<>();
    private String selectedCategory = null; // null = "All"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar();
        setupGreeting();
        setupRecyclerViews();
        setupViewModel();
        setupSwipeRefresh();
        setupFab();

        // Show shimmer initially
        binding.shimmer.setVisibility(View.VISIBLE);
        binding.shimmer.startShimmer();
    }

    // ── Toolbar ─────────────────────────────────────────────────

    private void setupToolbar() {
        binding.topAppBar.setTitle("");

        binding.toolbarSearch.setOnClickListener(v -> {
            // Navigate to Events tab with search focus
            if (getActivity() instanceof com.example.ieeeconnect.DashboardActivity) {
                // Switch to events tab (index 1)
                Toast.makeText(getContext(), "Search coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        binding.toolbarNotification.setOnClickListener(v ->
            Toast.makeText(getContext(), "No new notifications", Toast.LENGTH_SHORT).show()
        );
    }

    // ── Greeting ────────────────────────────────────────────────

    private void setupGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = "";
        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                name = displayName.split(" ")[0]; // first name
            } else if (user.getEmail() != null) {
                name = user.getEmail().split("@")[0];
            }
        }

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeGreeting;
        if (hour < 12) timeGreeting = "Good morning";
        else if (hour < 17) timeGreeting = "Good afternoon";
        else timeGreeting = "Good evening";

        if (!name.isEmpty()) {
            binding.tvGreeting.setText(timeGreeting + ", " + name + " \uD83D\uDC4B");
        } else {
            binding.tvGreeting.setText(timeGreeting + " \uD83D\uDC4B");
        }
    }

    // ── RecyclerViews ───────────────────────────────────────────

    private void setupRecyclerViews() {
        feedAdapter = new FeedAdapter();
        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recycler.setAdapter(feedAdapter);
        disableChangeAnimations(binding.recycler);

        featuredAdapter = new FeaturedAdapter();
        binding.featuredRecycler.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.featuredRecycler.setAdapter(featuredAdapter);
        disableChangeAnimations(binding.featuredRecycler);
    }

    // ── ViewModel ───────────────────────────────────────────────

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(EventsViewModel.class);

        viewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            if (binding == null) return;
            allEventsCache = events != null ? events : new ArrayList<>();

            // Stop loading states
            binding.swipeRefresh.setRefreshing(false);
            binding.shimmer.stopShimmer();
            binding.shimmer.setVisibility(View.GONE);
            binding.offlineBanner.setVisibility(isNetworkAvailable() ? View.GONE : View.VISIBLE);

            updateCategoryChips(allEventsCache);
            updateHero(allEventsCache);
            updateFeatured(allEventsCache);
            applyFeedFilter();
        });
    }

    // ── Category Chips ──────────────────────────────────────────

    private void updateCategoryChips(List<Event> events) {
        Set<String> categories = new HashSet<>();
        for (Event e : events) {
            if (e.getCategory() != null && !e.getCategory().isEmpty()) {
                categories.add(e.getCategory());
            }
        }

        binding.chipGroupCategories.removeAllViews();

        if (categories.isEmpty()) {
            binding.chipScrollView.setVisibility(View.GONE);
            return;
        }
        binding.chipScrollView.setVisibility(View.VISIBLE);

        // "All" chip
        Chip allChip = new Chip(requireContext());
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setChecked(selectedCategory == null);
        allChip.setOnClickListener(v -> {
            selectedCategory = null;
            applyFeedFilter();
            uncheckOtherChips(allChip);
        });
        binding.chipGroupCategories.addView(allChip);

        // Category chips
        for (String cat : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChecked(cat.equals(selectedCategory));
            chip.setOnClickListener(v -> {
                selectedCategory = cat;
                applyFeedFilter();
                uncheckOtherChips(chip);
            });
            binding.chipGroupCategories.addView(chip);
        }
    }

    private void uncheckOtherChips(Chip selected) {
        for (int i = 0; i < binding.chipGroupCategories.getChildCount(); i++) {
            View child = binding.chipGroupCategories.getChildAt(i);
            if (child instanceof Chip && child != selected) {
                ((Chip) child).setChecked(false);
            }
        }
        selected.setChecked(true);
    }

    // ── Hero ────────────────────────────────────────────────────

    private void updateHero(List<Event> events) {
        long now = System.currentTimeMillis();
        Event nextEvent = events.stream()
                .filter(e -> e.getStartTime() > now)
                .min(Comparator.comparingLong(Event::getStartTime))
                .orElse(null);

        if (nextEvent == null) {
            binding.heroContainer.setVisibility(View.GONE);
            if (heroTimer != null) heroTimer.cancel();
            return;
        }

        binding.heroContainer.setVisibility(View.VISIBLE);
        binding.heroTitle.setText(nextEvent.getTitle());

        if (nextEvent.getBannerUrl() != null && !nextEvent.getBannerUrl().isEmpty()) {
            Glide.with(this)
                    .load(nextEvent.getBannerUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(binding.heroBanner);
        }

        // Countdown timer
        if (heroTimer != null) heroTimer.cancel();
        long diff = Math.max(0, nextEvent.getStartTime() - now);
        heroTimer = new CountDownTimer(diff, 1000) {
            @Override
            public void onTick(long l) {
                if (binding != null) binding.heroCountdown.setText(formatCountdown(l));
            }
            @Override
            public void onFinish() {
                if (binding != null) binding.heroCountdown.setText(getString(R.string.starting_now));
            }
        }.start();

        // Navigate to detail on register click
        final String eventId = nextEvent.getEventId();
        binding.heroRegister.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EventDetailActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        // Tap hero card to open detail
        binding.heroContainer.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EventDetailActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
    }

    // ── Featured ────────────────────────────────────────────────

    private void updateFeatured(List<Event> events) {
        if (events.isEmpty()) {
            binding.sectionFeatured.setVisibility(View.GONE);
            return;
        }
        binding.sectionFeatured.setVisibility(View.VISIBLE);

        // Sort by going count (popularity) and take top 5
        List<Event> featured = events.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getGoingUserIds().size(),
                        a.getGoingUserIds().size()))
                .limit(5)
                .collect(Collectors.toList());

        featuredAdapter.submitList(featured);
    }

    // ── Feed Filter ─────────────────────────────────────────────

    private void applyFeedFilter() {
        List<Event> filtered;
        if (selectedCategory == null) {
            filtered = allEventsCache;
        } else {
            filtered = allEventsCache.stream()
                    .filter(e -> selectedCategory.equals(e.getCategory()))
                    .collect(Collectors.toList());
        }

        boolean isEmpty = filtered.isEmpty();
        binding.recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.sectionRecentLabel.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        feedAdapter.submitList(filtered);
    }

    // ── Swipe Refresh ───────────────────────────────────────────

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
                getResources().getColor(R.color.primary, null));
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refreshFromNetwork());
    }

    // ── FAB ─────────────────────────────────────────────────────

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v ->
                startActivity(new Intent(getContext(), CreateEventActivity.class)));
        checkAdminAndShowFab();
    }

    private void checkAdminAndShowFab() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        // Check users collection for admin/excom role
        firestore.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (binding == null) return;
            if (doc.exists()) {
                boolean isAdmin = Boolean.TRUE.equals(doc.getBoolean("isAdmin"));
                String role = doc.getString("role");
                boolean privileged = isAdmin
                        || "ADMIN".equalsIgnoreCase(role)
                        || "SUPER_ADMIN".equalsIgnoreCase(role)
                        || "EXCOM".equalsIgnoreCase(role);
                binding.fabAdd.setVisibility(privileged ? View.VISIBLE : View.GONE);
            } else {
                binding.fabAdd.setVisibility(View.GONE);
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String formatCountdown(long millis) {
        long seconds = millis / 1000;
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (d > 0) {
            return String.format(Locale.getDefault(), "Starts in %dd %02dh %02dm", d, h, m);
        }
        return String.format(Locale.getDefault(), "Starts in %02d:%02d:%02d", h, m, s);
    }

    private boolean isNetworkAvailable() {
        Context ctx = requireContext().getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private static void disableChangeAnimations(RecyclerView rv) {
        RecyclerView.ItemAnimator animator = rv.getItemAnimator();
        if (animator instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
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

    // ═══════════════════════════════════════════════════════════
    //  FEATURED ADAPTER
    // ═══════════════════════════════════════════════════════════

    private static class FeaturedAdapter extends androidx.recyclerview.widget.ListAdapter<Event, FeaturedAdapter.VH> {
        private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM dd", Locale.getDefault());

        FeaturedAdapter() {
            super(new DiffUtil.ItemCallback<Event>() {
                @Override public boolean areItemsTheSame(@NonNull Event o, @NonNull Event n) {
                    return o.getEventId().equals(n.getEventId());
                }
                @Override public boolean areContentsTheSame(@NonNull Event o, @NonNull Event n) {
                    return o.equals(n);
                }
            });
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_featured, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            h.bind(getItem(pos));
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView banner;
            TextView title, date, location;

            VH(@NonNull View v) {
                super(v);
                banner = v.findViewById(R.id.featured_image);
                title = v.findViewById(R.id.featured_title);
                date = v.findViewById(R.id.featured_date);
                location = v.findViewById(R.id.featured_location);
            }

            void bind(Event event) {
                title.setText(event.getTitle());

                // Date badge
                long time = event.getStartTime() > 0 ? event.getStartTime() : event.getEventTime();
                if (time > 0) {
                    date.setVisibility(View.VISIBLE);
                    date.setText(DATE_FMT.format(new Date(time)).toUpperCase());
                } else {
                    date.setVisibility(View.GONE);
                }

                // Location
                String loc = event.getLocationName();
                if (loc == null || loc.isEmpty()) loc = event.getLocation();
                if (loc != null && !loc.isEmpty()) {
                    location.setVisibility(View.VISIBLE);
                    location.setText(loc);
                } else {
                    location.setVisibility(View.GONE);
                }

                // Banner image
                Glide.with(banner.getContext())
                        .load(event.getBannerUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(banner);

                // Click → event detail
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
                    intent.putExtra("eventId", event.getEventId());
                    v.getContext().startActivity(intent);
                });
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  FEED ADAPTER
    // ═══════════════════════════════════════════════════════════

    private static class FeedAdapter extends androidx.recyclerview.widget.ListAdapter<Event, FeedAdapter.VH> {
        private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault());

        FeedAdapter() {
            super(new DiffUtil.ItemCallback<Event>() {
                @Override public boolean areItemsTheSame(@NonNull Event o, @NonNull Event n) {
                    return o.getEventId().equals(n.getEventId());
                }
                @Override public boolean areContentsTheSame(@NonNull Event o, @NonNull Event n) {
                    return o.equals(n);
                }
            });
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_feed_post, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            h.bind(getItem(pos));
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView banner;
            TextView title, description, time, likes;
            View btnGoing;

            VH(@NonNull View v) {
                super(v);
                banner = v.findViewById(R.id.post_image);
                title = v.findViewById(R.id.post_title);
                description = v.findViewById(R.id.post_body);
                time = v.findViewById(R.id.post_time);
                likes = v.findViewById(R.id.post_likes);
                btnGoing = v.findViewById(R.id.btn_going);
            }

            void bind(Event event) {
                title.setText(event.getTitle());
                description.setText(event.getDescription());

                // Time display
                long eventTime = event.getStartTime() > 0 ? event.getStartTime() : event.getEventTime();
                if (eventTime > 0) {
                    time.setText(TIME_FMT.format(new Date(eventTime)));
                } else if (event.getFormattedTime() != null) {
                    time.setText(event.getFormattedTime());
                } else {
                    time.setText("");
                }

                // Going count
                int goingCount = event.getGoingUserIds().size();
                if (goingCount > 0) {
                    likes.setText(goingCount + " going");
                } else {
                    likes.setText("");
                }

                // Update "Interested" button state for current user
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && btnGoing != null) {
                    boolean isGoing = event.getGoingUserIds().contains(user.getUid());
                    if (btnGoing instanceof com.google.android.material.button.MaterialButton) {
                        com.google.android.material.button.MaterialButton mb =
                                (com.google.android.material.button.MaterialButton) btnGoing;
                        if (isGoing) {
                            mb.setText("Going ✓");
                            mb.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                    mb.getContext().getResources().getColor(R.color.primary, null)));
                            mb.setTextColor(mb.getContext().getResources().getColor(R.color.white, null));
                        } else {
                            mb.setText("Interested");
                            mb.setBackgroundTintList(null);
                            mb.setTextColor(mb.getContext().getResources().getColor(R.color.primary, null));
                        }
                    }
                }

                // Banner image
                Glide.with(banner.getContext())
                        .load(event.getBannerUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(banner);

                // Click → event detail with shared element transition
                itemView.setOnClickListener(v -> {
                    EventDetailActivity.startWithTransition(
                            (AppCompatActivity) v.getContext(),
                            banner,
                            event.getEventId()
                    );
                });

                // Going button click
                if (btnGoing != null && user != null) {
                    btnGoing.setOnClickListener(v -> {
                        // Optimistic toggle — will be handled by ViewModel observer
                        Toast.makeText(v.getContext(), "Updated!", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }
    }
}
