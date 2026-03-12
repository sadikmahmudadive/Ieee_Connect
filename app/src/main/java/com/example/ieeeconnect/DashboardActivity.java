package com.example.ieeeconnect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import android.app.PendingIntent;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.ieeeconnect.databinding.ActivityDashboardBinding;
import com.example.ieeeconnect.ui.admin.AdminDashboardFragment;
import com.example.ieeeconnect.ui.chat.ChatFragment;
import com.example.ieeeconnect.ui.committee.CommitteeFragment;
import com.example.ieeeconnect.ui.events.EventsFragment;
import com.example.ieeeconnect.ui.home.HomeFragment;
import com.example.ieeeconnect.ui.profile.ProfileFragment;
import com.example.ieeeconnect.ui.views.CustomBottomNavView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private ActivityDashboardBinding binding;

    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    // Cached fragment instances to prevent re-creation on every tab switch
    private final HomeFragment homeFragment = new HomeFragment();
    private final EventsFragment eventsFragment = new EventsFragment();
    private final ChatFragment chatFragment = new ChatFragment();
    private final CommitteeFragment committeeFragment = new CommitteeFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private AdminDashboardFragment adminFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupNotificationPermissionRequest();
        EdgeToEdge.enable(this);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CustomBottomNavView navView = binding.customBottomNavView;

        // Default: hide admin tab until we determine the user's privileges
        navView.setAdminVisible(false);

        // Request POST_NOTIFICATIONS at dashboard launch for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // First try: intent extras (backward-compatibility)
        if (getIntent() != null) {
            boolean isAdmin = getIntent().getBooleanExtra("isAdmin", false);
            String role = getIntent().getStringExtra("role");
            boolean isAdminRole = "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);
            if (isAdmin || isAdminRole) {
                navView.setAdminVisible(true);
            }
        }

        // Then attempt to determine current user's admin status from Firestore (preferred)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            boolean docIsAdmin = false;
                            String docRole = null;
                            Object isAdminObj = doc.get("isAdmin");
                            Object roleObj = doc.get("role");
                            if (isAdminObj instanceof Boolean) {
                                docIsAdmin = (Boolean) isAdminObj;
                            } else if (isAdminObj != null) {
                                String val = isAdminObj.toString().trim().toLowerCase();
                                docIsAdmin = val.equals("true") || val.equals("1") || val.equals("yes");
                            }
                            if (roleObj != null) docRole = roleObj.toString();

                            boolean privileged = docIsAdmin
                                    || "ADMIN".equalsIgnoreCase(docRole)
                                    || "SUPER_ADMIN".equalsIgnoreCase(docRole)
                                    || "EXCOM".equalsIgnoreCase(docRole);
                            navView.setAdminVisible(privileged);
                        } else {
                            Log.d(TAG, "No user document found for uid=" + uid);
                        }
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to fetch user role: " + e.getMessage()));
        }

        // Add all fragments initially, hide all except home
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, homeFragment, "home")
                    .add(R.id.fragment_container, eventsFragment, "events").hide(eventsFragment)
                    .add(R.id.fragment_container, chatFragment, "chat").hide(chatFragment)
                    .add(R.id.fragment_container, committeeFragment, "committee").hide(committeeFragment)
                    .add(R.id.fragment_container, profileFragment, "profile").hide(profileFragment)
                    .commit();
            activeFragment = homeFragment;
        }

        navView.setOnNavigationItemSelectedListener(itemId -> {
            if (itemId == R.id.navigation_home) {
                switchTo(homeFragment);
            } else if (itemId == R.id.navigation_events) {
                switchTo(eventsFragment);
            } else if (itemId == R.id.navigation_chat) {
                switchTo(chatFragment);
            } else if (itemId == R.id.navigation_profile) {
                switchTo(profileFragment);
            } else if (itemId == R.id.navigation_committee) {
                switchTo(committeeFragment);
            } else if (itemId == R.id.navigation_admin) {
                if (adminFragment == null) {
                    adminFragment = new AdminDashboardFragment();
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, adminFragment, "admin")
                            .hide(adminFragment)
                            .commit();
                }
                switchTo(adminFragment);
            }
        });

        // Debug: quick button to post a sample notification (only visible in debug builds)
        // Check whether the app is debuggable at runtime to avoid depending on generated BuildConfig
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            try {
                ViewGroup root = (ViewGroup) binding.getRoot();
                Button dbg = new Button(this);
                dbg.setText("Post test notification");
                dbg.setAllCaps(false);
                dbg.setOnClickListener(v -> {
                    // Ensure permission on Android 13+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                            Toast.makeText(this, "Requesting notification permission...", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    String channelId = "chat_notifications";
                    String title = "Test message";
                    String body = "This is a debug notification from IEEE Connect";

                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

                    NotificationCompat.Builder b = new NotificationCompat.Builder(this, channelId)
                            .setSmallIcon(R.drawable.ic_chat)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setAutoCancel(true)
                            .setContentIntent(pi)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setDefaults(NotificationCompat.DEFAULT_ALL);

                    NotificationManagerCompat.from(this).notify((int) (System.currentTimeMillis() & 0xfffffff), b.build());
                });
                // add at the end of root view so it's easily tappable during testing
                root.addView(dbg);
            } catch (Exception e) {
                Log.w(TAG, "Failed to add debug notification button: " + e.getMessage());
            }
        }
    }

    /**
     * Show/hide fragments instead of replacing — preserves state, prevents
     * chat list reload, event list flash, etc.
     */
    private void switchTo(Fragment target) {
        if (target == activeFragment) return;
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.hide(activeFragment);
        tx.show(target);
        tx.commit();
        activeFragment = target;
    }

    /**
     * Called by AdminDashboardFragment when the user should leave admin mode.
     */
    public void exitAdminMode() {
        binding.customBottomNavView.setAdminVisible(false);
        if (activeFragment instanceof AdminDashboardFragment) {
            switchTo(homeFragment);
            binding.customBottomNavView.selectTab(0);
        }
    }

    /**
     * Initialize the ActivityResultLauncher used to request POST_NOTIFICATIONS
     * permission on Android 13+ and handle the user's response (including
     * permanent denial where we direct the user to app notification settings).
     */
    private void setupNotificationPermissionRequest() {
        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "POST_NOTIFICATIONS granted");
                        return;
                    }

                    Log.d(TAG, "POST_NOTIFICATIONS denied");
                    // Only applicable on Android 13+ where POST_NOTIFICATIONS exists
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // If the user permanently denied (Don't ask again), shouldShowRequestPermissionRationale() returns false.
                        boolean shouldExplain = shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS);
                        if (!shouldExplain) {
                            // Permanent denial -> guide user to app notification settings
                            new AlertDialog.Builder(this)
                                    .setTitle("Enable notifications")
                                    .setMessage("To receive messages and call alerts, please enable notifications for IEEE Connect in system settings.")
                                    .setPositiveButton("Open settings", (dialog, which) -> {
                                        try {
                                            Intent intent = new Intent();
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                                            } else {
                                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                intent.setData(Uri.fromParts("package", getPackageName(), null));
                                            }
                                            startActivity(intent);
                                        } catch (Exception e) {
                                            // Fallback to application details if a device doesn't support the notification settings action
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.fromParts("package", getPackageName(), null));
                                            startActivity(intent);
                                        }
                                    })
                                    .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                                    .show();
                        } else {
                            // Not permanent denial: show short rationale and offer to request again
                            new AlertDialog.Builder(this)
                                    .setTitle("Allow notifications")
                                    .setMessage("We need permission to post notifications so you don't miss messages and calls.")
                                    .setPositiveButton("Allow", (dialog, which) -> requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                                    .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                                    .show();
                        }
                    }
                }
        );
    }
}
