package com.example.ieeeconnect;

import android.app.Application;

import com.example.ieeeconnect.data.remote.CloudinaryManager;
import com.google.firebase.database.FirebaseDatabase;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CloudinaryManager.init(this);
        
        // Enable Firebase Realtime Database offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Ensure notification channel exists at startup so FCM can post reliably
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                String channelId = "chat_notifications";
                NotificationChannel existing = nm.getNotificationChannel(channelId);
                if (existing == null) {
                    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    NotificationChannel channel = new NotificationChannel(channelId, "Chat Notifications", NotificationManager.IMPORTANCE_HIGH);
                    AudioAttributes attrs = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    channel.setSound(soundUri, attrs);
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0,250,250,250});
                    nm.createNotificationChannel(channel);
                    Log.d("MyApplication", "Created notification channel: " + channelId);
                }
            }
        } catch (Exception e) {
            Log.e("MyApplication", "Failed to create notification channel: " + e.getMessage(), e);
        }
    }
}
