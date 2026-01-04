package com.example.ieeeconnect.ui.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.activities.CreateEventActivity;
import com.example.ieeeconnect.databinding.FragmentHomeBinding;
import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.viewmodels.EventsViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private FeedAdapter adapter;
    private EventsViewModel viewModel;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // Launcher to start CreateEventActivity and refresh feed on success
    private ActivityResultLauncher<Intent> createEventLauncher = registerForActivityResult(
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
        adapter = new FeedAdapter(new ArrayList<>());
        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recycler.setAdapter(adapter);

        // Featured carousel: simple horizontal list using same Event model for now
        binding.featuredRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        FeaturedAdapter featuredAdapter = new FeaturedAdapter(new ArrayList<>());
        binding.featuredRecycler.setAdapter(featuredAdapter);

        viewModel = new ViewModelProvider(this).get(EventsViewModel.class);
        // Observe and update both featured and main feed
        viewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            // stop shimmer
            binding.shimmer.stopShimmer();
            binding.shimmer.setVisibility(View.GONE);

            // update feed using DiffUtil
            adapter.setItems(events != null ? events : new ArrayList<>());

            // featured: pick first 5 events as featured
            List<Event> featured = (events != null && events.size() > 5) ? events.subList(0, 5) : (events != null ? events : new ArrayList<>());
            featuredAdapter.setItems(new ArrayList<>(featured));

            // hide swipe refresh if active
            SwipeRefreshLayout swipe = binding.getRoot().findViewById(com.example.ieeeconnect.R.id.swipe_refresh);
            if (swipe != null) swipe.setRefreshing(false);

            // offline banner visibility
            if (!isNetworkAvailable()) {
                binding.offlineBanner.setVisibility(View.VISIBLE);
            } else {
                binding.offlineBanner.setVisibility(View.GONE);
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
    }

    private void checkAdminAndShowFab() {
        // check if current user is in 'committee' collection or has an admin role
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("committee").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        binding.fabAdd.setVisibility(View.VISIBLE);
                    } else {
                        // fallback: check roles collection
                        firestore.collection("roles").document(uid).get()
                                .addOnSuccessListener(rdoc -> {
                                    if (rdoc != null && rdoc.exists() && Boolean.TRUE.equals(rdoc.getBoolean("isAdmin"))) {
                                        binding.fabAdd.setVisibility(View.VISIBLE);
                                    } else {
                                        binding.fabAdd.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> binding.fabAdd.setVisibility(View.GONE));
                    }
                })
                .addOnFailureListener(e -> binding.fabAdd.setVisibility(View.GONE));
    }

    private boolean isNetworkAvailable() {
        Context ctx = requireContext().getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    // kept for backwards compatibility; admin check is async and handled separately
    private boolean isCurrentUserAdmin() {
        return false;
    }

    // Simple featured adapter
    private static class FeaturedAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<FeaturedAdapter.VH> {
        private final List<Event> items;

        FeaturedAdapter(List<Event> items) {
            this.items = items;
        }

        void setItems(List<Event> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(com.example.ieeeconnect.R.layout.item_featured, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Event e = items.get(position);
            holder.title.setText(e.getTitle());
            // only title and image shown in featured layout
            Glide.with(holder.banner.getContext())
                    .load(e.getBannerUrl())
                    .placeholder(com.example.ieeeconnect.R.drawable.ic_launcher_background)
                    .into(holder.banner);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.ImageView banner;
            android.widget.TextView title;

            VH(@NonNull View itemView) {
                super(itemView);
                banner = itemView.findViewById(com.example.ieeeconnect.R.id.featured_image);
                title = itemView.findViewById(com.example.ieeeconnect.R.id.featured_title);
            }
        }
    }

    // Lightweight feed adapter with DiffUtil
    private class FeedAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<FeedAdapter.VH> {
        private final List<Event> items;

        FeedAdapter(List<Event> items) {
            this.items = items;
        }

        void setItems(List<Event> newItems) {
            // DiffUtil to update smoothly
            final List<Event> old = new ArrayList<>(items);
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() { return old.size(); }

                @Override
                public int getNewListSize() { return newItems.size(); }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return old.get(oldItemPosition).getEventId().equals(newItems.get(newItemPosition).getEventId());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    Event o = old.get(oldItemPosition);
                    Event n = newItems.get(newItemPosition);
                    return o.getTitle().equals(n.getTitle()) && o.getDescription().equals(n.getDescription()) && ((o.getBannerUrl()==null && n.getBannerUrl()==null) || (o.getBannerUrl()!=null && o.getBannerUrl().equals(n.getBannerUrl())));
                }
            });
            items.clear();
            items.addAll(newItems);
            diff.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(com.example.ieeeconnect.R.layout.item_feed_post, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Event e = items.get(position);
            holder.title.setText(e.getTitle());
            holder.description.setText(e.getDescription());
            holder.time.setText(android.text.format.DateFormat.format("dd MMM yyyy hh:mm a", e.getEventTime()));
            Glide.with(holder.banner.getContext()).load(e.getBannerUrl()).placeholder(com.example.ieeeconnect.R.drawable.ic_launcher_background).into(holder.banner);

            // Simple like/going state display — use string resource for localization
            int count = e.getGoingUserIds() != null ? e.getGoingUserIds().size() : 0;
            holder.likes.setText(holder.likes.getContext().getString(R.string.going_count, count));

            // update button text based on whether current user is going or interested
            String uid = null;
            try {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            } catch (Exception ex) { /* ignore unauthenticated state */ }

            final String userId = uid; // effectively final for lambda capture
            final String eventIdForLambda = e.getEventId();

            boolean isGoing = userId != null && e.getGoingUserIds() != null && e.getGoingUserIds().contains(userId);
            boolean isInterested = userId != null && e.getInterestedUserIds() != null && e.getInterestedUserIds().contains(userId);

            if (isGoing) {
                holder.btnGoing.setText(holder.btnGoing.getContext().getString(R.string.btn_going));
            } else if (isInterested) {
                holder.btnGoing.setText(holder.btnGoing.getContext().getString(R.string.btn_interested));
            } else {
                holder.btnGoing.setText(holder.btnGoing.getContext().getString(R.string.btn_not_going));
            }

            holder.btnGoing.setOnClickListener(v -> {
                // simple pop animation for the button view
                ScaleAnimation anim = new ScaleAnimation(
                        0.8f, 1.0f,
                        0.8f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                anim.setDuration(220);
                v.startAnimation(anim);

                if (viewModel != null && userId != null) {
                    // play lottie animation overlay
                    if (holder.likeAnim != null) {
                        holder.likeAnim.setVisibility(View.VISIBLE);
                        holder.likeAnim.playAnimation();
                        holder.likeAnim.addAnimatorListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                holder.likeAnim.setVisibility(View.GONE);
                                holder.likeAnim.removeAllAnimatorListeners();
                            }
                        });
                    }

                    // toggle going optimistically
                    viewModel.toggleGoing(eventIdForLambda, userId);
                } else {
                    Toast.makeText(v.getContext(), "Sign in to RSVP", Toast.LENGTH_SHORT).show();
                }
            });

            // long-press to toggle Interested state
            holder.btnGoing.setOnLongClickListener(v -> {
                if (viewModel != null && userId != null) {
                    // show quick feedback
                    Toast.makeText(v.getContext(), "Toggling Interested...", Toast.LENGTH_SHORT).show();
                    if (holder.likeAnim != null) {
                        holder.likeAnim.setVisibility(View.VISIBLE);
                        holder.likeAnim.playAnimation();
                        holder.likeAnim.addAnimatorListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                holder.likeAnim.setVisibility(View.GONE);
                                holder.likeAnim.removeAllAnimatorListeners();
                            }
                        });
                    }
                    viewModel.toggleInterested(eventIdForLambda, userId);
                    return true;
                } else {
                    Toast.makeText(v.getContext(), "Sign in to RSVP", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
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
            }
        }
    }
}
