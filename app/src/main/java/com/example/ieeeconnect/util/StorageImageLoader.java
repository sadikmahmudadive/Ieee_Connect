package com.example.ieeeconnect.util;

import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Map;

/**
 * Helper to load images referenced either by HTTP(S) URL or Firebase Storage path (gs:// or relative)
 * and cache resolved HTTP URLs when a cache map & key are provided.
 */
public class StorageImageLoader {
    private static final String TAG = "StorageImageLoader";

    public static void load(ImageView imageView, String photoPath, Map<String, String> cache, String cacheKey, @DrawableRes int placeholderRes) {
        if (imageView == null) return;

        // Reset placeholder and clear prior requests
        imageView.setImageResource(placeholderRes != 0 ? placeholderRes : R.drawable.ic_profile_placeholder);
        try { Glide.with(imageView.getContext()).clear(imageView); } catch (Exception ignored) {}

        if (photoPath == null || photoPath.isEmpty()) {
            return;
        }

        // If it's already a web URL, load directly
        if (photoPath.startsWith("http")) {
            if (cache != null && cacheKey != null) cache.put(cacheKey, photoPath);
            try {
                Glide.with(imageView.getContext())
                        .load(photoPath)
                        .placeholder(placeholderRes)
                        .error(placeholderRes)
                        .circleCrop()
                        .into(imageView);
            } catch (Exception e) {
                imageView.setImageResource(placeholderRes);
            }
            return;
        }

        // If cache provided and contains a resolved URL, use it
        if (cache != null && cacheKey != null && cache.containsKey(cacheKey)) {
            String cached = cache.get(cacheKey);
            if (cached != null && cached.startsWith("http")) {
                try {
                    Glide.with(imageView.getContext())
                            .load(cached)
                            .placeholder(placeholderRes)
                            .error(placeholderRes)
                            .circleCrop()
                            .into(imageView);
                    return;
                } catch (Exception e) {
                    // fallthrough to resolve
                }
            }
        }

        // Resolve via Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference ref;
        try {
            if (photoPath.startsWith("gs://") || photoPath.contains("firebasestorage.googleapis.com")) {
                ref = storage.getReferenceFromUrl(photoPath);
            } else {
                ref = storage.getReference().child(photoPath);
            }
        } catch (Exception e) {
            Log.w(TAG, "Invalid storage reference: " + photoPath, e);
            imageView.setImageResource(placeholderRes);
            if (cache != null && cacheKey != null) cache.put(cacheKey, null);
            return;
        }

        final String finalCacheKey = cacheKey;
        final Map<String, String> finalCache = cache;
        ref.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    if (finalCache != null && finalCacheKey != null) finalCache.put(finalCacheKey, url);
                    try {
                        Glide.with(imageView.getContext())
                                .load(url)
                                .placeholder(placeholderRes)
                                .error(placeholderRes)
                                .circleCrop()
                                .into(imageView);
                    } catch (Exception e) {
                        imageView.setImageResource(placeholderRes);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to resolve storage url: " + photoPath, e);
                    imageView.setImageResource(placeholderRes);
                    if (finalCache != null && finalCacheKey != null) finalCache.put(finalCacheKey, null);
                });
    }
}

