package com.example.ieeeconnect.data.remote;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.ieeeconnect.R;
import com.example.ieeeconnect.ui.chat.CallScreenActivity;
import com.example.ieeeconnect.ui.chat.ChatRoomActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FCMService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "chat_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d("FCMService", "onMessageReceived: from=" + remoteMessage.getFrom()
                + " messageId=" + remoteMessage.getMessageId()
                + " dataPresent=" + (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty())
                + " notificationPresent=" + (remoteMessage.getNotification() != null));

        // Prefer data payload for routing (our FCM messages should use data payloads)
        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            String type = data.get("type");
            // If server didn't include a 'type' or provided a non-CALL type, treat as chat MESSAGE by default
            if ("CALL".equalsIgnoreCase(type)) {
                handleIncomingCall(data);
                return;
            } else {
                Log.d("FCMService", "data payload -> treating as MESSAGE (type='" + type + "')");
                showNewMessageNotification(data);
                return;
            }
        }

        // If there's a notification payload (sent by console), show a simple notification
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d("FCMService", "notification payload present -> showFallbackNotification");
            showFallbackNotification(title, body);
        }
    }

    /**
     * Ensure the notification channel exists (O+) with correct sound/vibration settings.
     * Returns true when channel is available/created successfully, false otherwise.
     */
    private boolean ensureNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationChannel existing = notificationManager.getNotificationChannel(CHANNEL_ID);
            boolean recreate = false;
            if (existing != null) {
                Uri existingSound = existing.getSound();
                if (existingSound == null || !existingSound.equals(soundUri)) recreate = true;
            }
            try {
                if (existing == null || recreate) {
                    if (existing != null) notificationManager.deleteNotificationChannel(CHANNEL_ID);
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Chat Notifications", NotificationManager.IMPORTANCE_HIGH);
                    AudioAttributes attrs = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    channel.setSound(soundUri, attrs);
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 250, 250, 250});
                    // Use brand color for channel lights where supported
                    try {
                        channel.enableLights(true);
                        channel.setLightColor(Color.parseColor("#FF386BF6"));
                    } catch (Exception ignored) {}
                    notificationManager.createNotificationChannel(channel);
                    Log.d("FCMService", "Notification channel created/recreated: " + CHANNEL_ID);
                }
                return true;
            } catch (Exception e) {
                Log.w("FCMService", "Failed to create notification channel: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private void handleIncomingCall(Map<String, String> data) {
        Intent intent = new Intent(this, CallScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("roomId", data.get("roomId"));
        intent.putExtra("roomName", data.get("senderName"));
        intent.putExtra("roomImage", data.get("senderImage"));
        intent.putExtra("isVideo", Boolean.parseBoolean(data.get("isVideo")));
        startActivity(intent);
    }

    private void showNewMessageNotification(Map<String, String> data) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationManagerCompat nm = NotificationManagerCompat.from(this);

        // If notifications are disabled for the app globally, log and bail out
        if (!nm.areNotificationsEnabled()) {
            Log.w("FCMService", "Notifications are disabled for this app (user blocked). Cannot show message notification.");
            return;
        }

        // On Android 13+, ensure we have POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("FCMService", "POST_NOTIFICATIONS permission not granted; skipping notification");
                // Permission not granted; skip showing notification
                return;
            }
        }

        Log.d("FCMService", "Showing notification for message from: " + data.get("senderName"));
        // Use default notification sound
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean ok = ensureNotificationChannel(notificationManager);
            if (!ok) {
                Log.w("FCMService", "Notification channel unavailable; notification may not play sound or may be blocked.");
            }
        }

        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("roomId", data.get("roomId"));
        intent.putExtra("roomName", data.get("senderName"));

        int requestCode = 0;
        try {
            String roomId = data.get("roomId");
            if (roomId != null) requestCode = Math.abs(roomId.hashCode());
            else requestCode = (int) (System.currentTimeMillis() & 0xfffffff);
        } catch (Exception e) {
            requestCode = (int) (System.currentTimeMillis() & 0xfffffff);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(data.get("senderName"))
                .setContentText(data.get("message"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setColor(Color.parseColor("#FF386BF6"))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(data.get("message")));

        // Use roomId-based notification id so notifications from same chat update instead of stacking
        int notifId;
        try {
            String roomId = data.get("roomId");
            if (roomId != null) notifId = Math.abs(roomId.hashCode());
            else notifId = (int) (System.currentTimeMillis() & 0xfffffff);
        } catch (Exception e) {
            notifId = (int) (System.currentTimeMillis() & 0xfffffff);
        }

        nm.notify(notifId, builder.build());
    }

    private void showFallbackNotification(String title, String body) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(this);

        if (!nm.areNotificationsEnabled()) {
            Log.w("FCMService", "Notifications are disabled for this app (user blocked). Skipping fallback notification.");
            return;
        }
        // simple fallback notification that opens the main dashboard
        Intent intent = new Intent(this, com.example.ieeeconnect.DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(title == null ? getString(R.string.app_name) : title)
                .setContentText(body == null ? "" : body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // On Android 13+, ensure POST_NOTIFICATIONS permission is granted before notifying
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("FCMService", "POST_NOTIFICATIONS permission not granted; skipping fallback notification");
                return;
            }
        }

        nm.notify((int) (System.currentTimeMillis() & 0xfffffff), builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCMService", "onNewToken: " + token);
        updateTokenInFirestore(token);
    }

    private void updateTokenInFirestore(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("fcmToken", token);
        }
    }
}
