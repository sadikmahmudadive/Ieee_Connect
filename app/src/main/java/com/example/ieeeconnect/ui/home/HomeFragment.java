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
    private EventsViewModel viewModel;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private CountDownTimer heroTimer;

    // Launcher to start CreateEventActivity and refresh feed on success
    private final ActivityResultLauncher<Intent> createEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (ActivityResult result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Refresh the feed to show newly created event
                    if (viewModel != null) viewModel.refreshFromNetwork();
                    Toast.makeText(requireContext(), "Event created — refreshing feed", Toast.LENGTH_SHORT).show();
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

        // Setup toolbar
        binding.topAppBar.setTitle(""); // Clear default title
        binding.toolbarLogo.setImageResource(R.drawable.ic_ieee_logo); // Set IEEE BUBT logo
        binding.toolbarSearch.setOnClickListener(v -> {
            // Handle search action
            Toast.makeText(requireContext(), "Search clicked", Toast.LENGTH_SHORT).show();
        });
        binding.toolbarNotification.setOnClickListener(v -> {
            // Handle notification action
            Toast.makeText(requireContext(), "Notifications clicked", Toast.LENGTH_SHORT).show();
        });

        // Use ListAdapter-based FeedAdapter to leverage AsyncListDiffer for background diffs and minimal UI rebinds
        adapter = new FeedAdapter();
        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        // Turn off default change animations to avoid flashing on updates
        // Disable change animations but keep default animator for add/remove performance
        androidx.recyclerview.widget.RecyclerView.ItemAnimator animator = binding.recycler.getItemAnimator();
        if (animator instanceof androidx.recyclerview.widget.SimpleItemAnimator) {
            ((androidx.recyclerview.widget.SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        binding.recycler.setItemAnimator(animator);
        // keep a larger view cache to reduce rebinding when scrolling
        binding.recycler.setItemViewCacheSize(20);
        binding.recycler.setAdapter(adapter);

        // Featured carousel: simple horizontal list using same Event model for now
        binding.featuredRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        FeaturedAdapter featuredAdapter = new FeaturedAdapter();
        binding.featuredRecycler.setAdapter(featuredAdapter);

        viewModel = new ViewModelProvider(this).get(EventsViewModel.class);
        // Observe and update both featured and main feed
        viewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            if (events == null || events.isEmpty()) {
                binding.recycler.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.VISIBLE);
            } else {
                binding.recycler.setVisibility(View.VISIBLE);
                binding.emptyState.setVisibility(View.GONE);
                // Use submitList on ListAdapter which does diffing on background thread
                adapter.submitList(new ArrayList<>(events));

                // Update featured carousel
                List<Event> featured = events.size() > 5 ? events.subList(0, 5) : events;
                featuredAdapter.submitList(new ArrayList<>(featured));

                // Update hero banner
                Event nextEvent = events.stream()
                        .filter(event -> event.getStartTime() > System.currentTimeMillis())
                        .min(Comparator.comparingLong(Event::getStartTime))
                        .orElse(null);

                if (nextEvent != null) {
                    binding.heroContainer.setVisibility(View.VISIBLE);
                    binding.heroTitle.setText(nextEvent.getTitle());
                    Glide.with(binding.heroBanner.getContext())
                            .load(nextEvent.getBannerUrl())
                            .placeholder(R.drawable.ic_launcher_background)
                            .into(binding.heroBanner);
                } else {
                    binding.heroContainer.setVisibility(View.GONE);
                }
            }

            // hide swipe refresh if active
            SwipeRefreshLayout swipe = binding.getRoot().findViewById(com.example.ieeeconnect.R.id.swipe_refresh);
            if (swipe != null) swipe.setRefreshing(false);

            // offline banner visibility
            if (!isNetworkAvailable()) {
                binding.offlineBanner.setVisibility(View.VISIBLE);
            } else {
                binding.offlineBanner.setVisibility(View.GONE);
            }

            // New feature: Display event categories
            Set<String> categories = new HashSet<>();
            if (events != null) {
                for (Event event : events) {
                    if (event.getCategory() != null) categories.add(event.getCategory());
                }
            }
            binding.categoriesTextView.setText(TextUtils.join(", ", categories));

            // Hero banner: show next upcoming event if any
            if (events != null && !events.isEmpty()) {
                Event next = null;
                long now = System.currentTimeMillis();
                for (Event e : events) {
                    if (e.getStartTime() > now) {
                        if (next == null || e.getStartTime() < next.getStartTime()) next = e;
                    }
                }
                if (next == null) {
                    binding.heroContainer.setVisibility(View.GONE);
                } else {
                    binding.heroContainer.setVisibility(View.VISIBLE);
                    binding.heroTitle.setText(next.getTitle());
                    Glide.with(binding.heroBanner.getContext())
                            .load(next.getBannerUrl())
                            .placeholder(R.drawable.ic_launcher_background)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                            .dontAnimate()
                            .into(binding.heroBanner);

                    // Setup countdown
                    if (heroTimer != null) heroTimer.cancel();
                    long diff = Math.max(0, next.getStartTime() - now);
                    binding.heroCountdown.setText(formatCountdown(diff));
                    heroTimer = new CountDownTimer(diff, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            binding.heroCountdown.setText(formatCountdown(millisUntilFinished));
                        }

                        @Override
                        public void onFinish() {
                            // Use string resource for localization
                            binding.heroCountdown.setText(getString(R.string.starting_now));
                        }
                    };
                    heroTimer.start();

                    // Register button goes to EventDetailActivity
                    final String nextId = next.getEventId();
                    binding.heroRegister.setOnClickListener(v -> {
                        Intent intent = new Intent(requireContext(), EventDetailActivity.class);
                        intent.putExtra("eventId", nextId);
                        startActivity(intent);
                    });
                }
            } else {
                binding.heroContainer.setVisibility(View.GONE);
            }
        });

        // Pull-to-refresh triggers a network fetch
        SwipeRefreshLayout swipe = binding.getRoot().findViewById(com.example.ieeeconnect.R.id.swipe_refresh);
        if (swipe != null) {
            swipe.setOnRefreshListener(() -> {
                // show shimmer while loading
                binding.shimmer.setVisibility(View.VISIBLE);
                binding.shimmer.startShimmer();
                // trigger viewModel refresh
                if (viewModel != null) viewModel.refreshFromNetwork();
            });
        }

        // show shimmer initially
        binding.shimmer.setVisibility(View.VISIBLE);
        binding.shimmer.startShimmer();

        // FAB visible only for admins - initially hidden until async check completes
        binding.fabAdd.setVisibility(View.GONE);
        binding.fabAdd.setOnClickListener(v -> {
            // start CreateEventActivity via launcher so we can refresh on result
            Intent i = new Intent(requireContext(), CreateEventActivity.class);
            createEventLauncher.launch(i);
        });

        checkAdminAndShowFab();

        // Corrected SwipeRefreshLayout reference
        binding.swipeRefresh.setOnRefreshListener(() -> {
            if (viewModel != null) {
                viewModel.refreshFromNetwork();
                Toast.makeText(requireContext(), "Refreshing feed...", Toast.LENGTH_SHORT).show();
            } else {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Unable to refresh feed.", Toast.LENGTH_SHORT).show();
            }
        });

        // Removed duplicate observer that caused redundant updates and UI flashing. The single observer above handles empty state, hero, featured carousel and offline banner.
    }

    private String formatCountdown(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        // Specify Locale in String.format
        return String.format(Locale.getDefault(), "Starts in %02d:%02d:%02d", h, m, s);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Auto-reload events when fragment is resumed
        if (viewModel != null) {
            viewModel.refreshFromNetwork();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (heroTimer != null) heroTimer.cancel();
        binding = null;
    }

    private void checkAdminAndShowFab() {
        // check if current user is in 'committee' collection or has an admin role
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("committee").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return; // Check if binding is still valid
                    if (doc != null && doc.exists()) {
                        binding.fabAdd.setVisibility(View.VISIBLE);
                    } else {
                        // fallback: check roles collection
                        firestore.collection("roles").document(uid).get()
                                .addOnSuccessListener(rdoc -> {
                                    if (binding == null) return; // Check if binding is still valid
                                    if (rdoc != null && rdoc.exists() && Boolean.TRUE.equals(rdoc.getBoolean("isAdmin"))) {
                                        binding.fabAdd.setVisibility(View.VISIBLE);
                                    } else {
                                        binding.fabAdd.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (binding != null) binding.fabAdd.setVisibility(View.GONE);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding != null) binding.fabAdd.setVisibility(View.GONE);
                });
    }

    private boolean isNetworkAvailable() {
        Context ctx = requireContext().getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    // Simple featured adapter using ListAdapter/AsyncListDiffer
    private static class FeaturedAdapter extends androidx.recyclerview.widget.ListAdapter<Event, FeaturedAdapter.VH> {
        protected FeaturedAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                 @Override
                 public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
                     return oldItem.getEventId().equals(newItem.getEventId());
                 }

                 @Override
                 public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
                     // compare key displayed fields to avoid unnecessary rebinds
                     boolean sameTitle = java.util.Objects.equals(oldItem.getTitle(), newItem.getTitle());
                     boolean sameBanner = java.util.Objects.equals(oldItem.getBannerUrl(), newItem.getBannerUrl());
                     boolean sameDesc = java.util.Objects.equals(oldItem.getDescription(), newItem.getDescription());
                     boolean sameLikes = oldItem.getLikes() == newItem.getLikes();
                     boolean sameTime = java.util.Objects.equals(oldItem.getFormattedTime(), newItem.getFormattedTime());
                     return sameTitle && sameBanner && sameDesc && sameLikes && sameTime;
                 }
             });
             setHasStableIds(true);
         }

        @Override
        public long getItemId(int position) {
            Event e = getItem(position);
            return e != null ? e.getEventId().hashCode() : RecyclerView.NO_ID;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(com.example.ieeeconnect.R.layout.item_featured, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Event e = getItem(position);
            holder.bind(e);
        }

        static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.ImageView banner;
            android.widget.TextView title;

            VH(@NonNull View itemView) {
                super(itemView);
                banner = itemView.findViewById(com.example.ieeeconnect.R.id.featured_image);
                title = itemView.findViewById(com.example.ieeeconnect.R.id.featured_title);
            }

            void bind(Event e) {
                title.setText(e.getTitle());
                Glide.with(banner.getContext())
                        .load(e.getBannerUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                        .dontAnimate()
                        .into(banner);
            }
        }
    }

    // Feed adapter migrated to ListAdapter for background diffs and minimal rebinds
    private static class FeedAdapter extends androidx.recyclerview.widget.ListAdapter<Event, FeedAdapter.VH> {
        protected FeedAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
                    return oldItem.getEventId().equals(newItem.getEventId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Event oldItem, @NonNull Event newItem) {
                    boolean sameTitle = java.util.Objects.equals(oldItem.getTitle(), newItem.getTitle());
                    boolean sameBanner = java.util.Objects.equals(oldItem.getBannerUrl(), newItem.getBannerUrl());
                    boolean sameDesc = java.util.Objects.equals(oldItem.getDescription(), newItem.getDescription());
                    boolean sameLikes = oldItem.getLikes() == newItem.getLikes();
                    boolean sameTime = java.util.Objects.equals(oldItem.getFormattedTime(), newItem.getFormattedTime());
                    return sameTitle && sameBanner && sameDesc && sameLikes && sameTime;
                }

                @Nullable
                @Override
                public Object getChangePayload(@NonNull Event oldItem, @NonNull Event newItem) {
                    android.os.Bundle payload = new android.os.Bundle();
                    if (!java.util.Objects.equals(oldItem.getTitle(), newItem.getTitle())) payload.putBoolean("title", true);
                    if (!java.util.Objects.equals(oldItem.getBannerUrl(), newItem.getBannerUrl())) payload.putBoolean("banner", true);
                    if (!java.util.Objects.equals(oldItem.getDescription(), newItem.getDescription())) payload.putBoolean("desc", true);
                    if (oldItem.getLikes() != newItem.getLikes()) payload.putBoolean("likes", true);
                    if (!java.util.Objects.equals(oldItem.getFormattedTime(), newItem.getFormattedTime())) payload.putBoolean("time", true);
                    return payload.isEmpty() ? null : payload;
                }
            });
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            Event e = getItem(position);
            return e != null ? e.getEventId().hashCode() : androidx.recyclerview.widget.RecyclerView.NO_ID;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_post, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Event event = getItem(position);
            holder.bind(event);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position, @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position);
                return;
            }
            Object payload = payloads.get(payloads.size() - 1);
            if (payload instanceof android.os.Bundle b) {
                Event event = getItem(position);
                if (b.getBoolean("title", false)) holder.title.setText(event.getTitle());
                if (b.getBoolean("desc", false)) holder.description.setText(event.getDescription());
                if (b.getBoolean("time", false)) holder.time.setText(event.getFormattedTime());
                if (b.getBoolean("likes", false)) holder.likes.setText(String.valueOf(event.getLikes()));
                if (b.getBoolean("banner", false)) {
                    Glide.with(holder.banner.getContext())
                            .load(event.getBannerUrl())
                            .placeholder(R.drawable.ic_launcher_background)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                            .dontAnimate()
                            .into(holder.banner);
                }
            } else {
                onBindViewHolder(holder, position);
            }
        }

        static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.ImageView banner;
            android.widget.TextView title;
            android.widget.TextView description;
            android.widget.TextView time;
            android.widget.TextView likes;
            android.widget.Button btnGoing;
            LottieAnimationView likeAnim;

            VH(@NonNull View itemView) {
                super(itemView);
                banner = itemView.findViewById(com.example.ieeeconnect.R.id.post_image);
                title = itemView.findViewById(com.example.ieeeconnect.R.id.post_title);
                description = itemView.findViewById(com.example.ieeeconnect.R.id.post_body);
                time = itemView.findViewById(com.example.ieeeconnect.R.id.post_time);
                likes = itemView.findViewById(com.example.ieeeconnect.R.id.post_likes);
                btnGoing = itemView.findViewById(com.example.ieeeconnect.R.id.btn_going);
                likeAnim = itemView.findViewById(com.example.ieeeconnect.R.id.like_anim);

                // Keep listeners lightweight and avoid re-creating heavy objects on each bind
                btnGoing.setOnClickListener(v -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    androidx.recyclerview.widget.RecyclerView.Adapter<?> a = getBindingAdapter();
                    if (!(a instanceof FeedAdapter)) return;
                    FeedAdapter feed = (FeedAdapter) a;
                    List<Event> list = feed.getCurrentList();
                    if (pos < 0 || pos >= list.size()) return;
                    Event ev = list.get(pos);
                    if (ev == null) return;
                    // placeholder action: show a toast
                    Toast.makeText(itemView.getContext(), "Marked 'Going' for: " + ev.getTitle(), Toast.LENGTH_SHORT).show();
                });

                likeAnim.setOnClickListener(v -> {
                    int pos = getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    androidx.recyclerview.widget.RecyclerView.Adapter<?> a = getBindingAdapter();
                    if (!(a instanceof FeedAdapter)) return;
                    FeedAdapter feed = (FeedAdapter) a;
                    List<Event> list = feed.getCurrentList();
                    if (pos < 0 || pos >= list.size()) return;
                    Event ev = list.get(pos);
                    if (ev == null) return;
                    // play animation locally and notify viewmodel to toggle like (placeholder)
                    likeAnim.playAnimation();
                    Toast.makeText(itemView.getContext(), "Liked: " + ev.getTitle(), Toast.LENGTH_SHORT).show();
                });
             }

             void bind(Event event) {
                 title.setText(event.getTitle());
                 description.setText(event.getDescription());
                 time.setText(event.getFormattedTime());
                 likes.setText(String.valueOf(event.getLikes()));

                 Glide.with(banner.getContext())
                         .load(event.getBannerUrl())
                         .placeholder(R.drawable.ic_launcher_background)
                         .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                         .dontAnimate()
                         .into(banner);
             }
         }
     }
 }

