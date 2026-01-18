package com.example.ieeeconnect.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.ieeeconnect.databinding.ItemOnboardingBinding;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {
    private final List<OnboardingPage> pages;

    public OnboardingAdapter(List<OnboardingPage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOnboardingBinding binding = ItemOnboardingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        OnboardingPage page = pages.get(position);
        
        // Fixed: Use correct binding names from updated XML IDs
        holder.binding.onboardingTitle.setText(page.getTitle());
        holder.binding.onboardingDescription.setText(page.getSubtitle());
        
        LottieAnimationView lav = holder.binding.onboardingAnimation;
        lav.setAnimation(page.getAnimationRes());
        lav.playAnimation();
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemOnboardingBinding binding;
        VH(ItemOnboardingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
