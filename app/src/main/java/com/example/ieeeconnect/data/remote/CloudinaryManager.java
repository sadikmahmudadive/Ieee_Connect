package com.example.ieeeconnect.data.remote;

import android.content.Context;
import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {

    private static final String CLOUD_NAME = "dhm0edatk";
    private static final String API_KEY = "879315316647413";
    private static final String API_SECRET = "BgrjuKuPR_UqGZf2Gb5RHKDmF_0";

    public static void init(Context context) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", CLOUD_NAME);
        config.put("api_key", API_KEY);
        config.put("api_secret", API_SECRET);
        MediaManager.init(context, config);
    }

    public static void upload(Uri fileUri, UploadCallback callback) {
        MediaManager.get().upload(fileUri).callback(callback).dispatch();
    }
}
