package com.example.ieeeconnect.data.remote;

import android.content.Context;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {
    public static void init(Context context, String cloudName, String apiKey, String apiSecret) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        MediaManager.init(context, config);
    }

    public static void upload(String filePath, UploadCallback callback) {
        MediaManager.get().upload(filePath).callback(callback).dispatch();
    }
}

