package com.example.ieeeconnect.data.remote;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.ieeeconnect.R;
import com.example.ieeeconnect.ui.chat.CallScreenActivity;
import com.example.ieeeconnect.ui.chat.ChatRoomActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FCMService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "chat_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");

        if ("CALL".equals(type)) {
            handleIncomingCall(data);
        } else if ("MESSAGE".equals(type)) {
            showNewMessageNotification(data);
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Chat Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("roomId", data.get("roomId"));
        intent.putExtra("roomName", data.get("senderName"));
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(data.get("senderName"))
                .setContentText(data.get("message"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
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
