package com.example.ieeeconnect.ui.admin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ieeeconnect.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QrScannerActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeScanner;
    private MaterialCardView resultCard;
    private ImageView ivResultIcon;
    private TextView tvResultTitle, tvResultDetail;
    private MaterialButton btnScanNext, btnSelectEvent;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String selectedEventId = null;
    private String selectedEventTitle = null;

    private final ActivityResultLauncher<String> cameraPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    startScanning();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        barcodeScanner = findViewById(R.id.barcodeScanner);
        resultCard = findViewById(R.id.resultCard);
        ivResultIcon = findViewById(R.id.ivResultIcon);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvResultDetail = findViewById(R.id.tvResultDetail);
        btnScanNext = findViewById(R.id.btnScanNext);
        btnSelectEvent = findViewById(R.id.btnSelectEvent);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnScanNext.setOnClickListener(v -> {
            resultCard.setVisibility(View.GONE);
            barcodeScanner.setVisibility(View.VISIBLE);
            barcodeScanner.resume();
            barcodeScanner.decodeSingle(scanCallback);
        });

        btnSelectEvent.setOnClickListener(v -> showEventPicker());

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScanning() {
        barcodeScanner.setVisibility(View.VISIBLE);
        barcodeScanner.resume();
        barcodeScanner.decodeSingle(scanCallback);
    }

    private final BarcodeCallback scanCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result == null || result.getText() == null) return;
            String scannedUid = result.getText().trim();
            barcodeScanner.pause();
            processScannedUid(scannedUid);
        }
    };

    private void processScannedUid(String uid) {
        // Look up user in Firestore
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        if (name == null || name.isEmpty()) name = doc.getString("name");
                        if (name == null) name = "Member";
                        recordAttendance(uid, name);
                    } else {
                        showResult(false, "User Not Found",
                                "No user found with ID:\n" + uid);
                    }
                })
                .addOnFailureListener(e -> showResult(false, "Error",
                        "Failed to look up user: " + e.getMessage()));
    }

    private void recordAttendance(String userId, String userName) {
        String adminUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown";

        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("userName", userName);
        record.put("timestamp", System.currentTimeMillis());
        record.put("recordedBy", adminUid);

        if (selectedEventId != null) {
            record.put("eventId", selectedEventId);
            record.put("eventTitle", selectedEventTitle);
        }

        db.collection("attendance").add(record)
                .addOnSuccessListener(ref -> {
                    String detail = "✅ " + userName;
                    if (selectedEventTitle != null) {
                        detail += "\nEvent: " + selectedEventTitle;
                    }
                    showResult(true, "Attendance Recorded", detail);
                })
                .addOnFailureListener(e -> showResult(false, "Failed",
                        "Could not record attendance: " + e.getMessage()));
    }

    private void showResult(boolean success, String title, String detail) {
        barcodeScanner.setVisibility(View.GONE);
        resultCard.setVisibility(View.VISIBLE);

        tvResultTitle.setText(title);
        tvResultDetail.setText(detail);

        if (success) {
            ivResultIcon.setImageResource(R.drawable.ic_check_circle);
            ivResultIcon.setColorFilter(0xFF2E7D32);
        } else {
            ivResultIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            ivResultIcon.setColorFilter(0xFFE65100);
        }
    }

    // ── Event Picker ────────────────────────────────────────────

    private void showEventPicker() {
        db.collection("events")
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snap -> {
                    List<String> titles = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    titles.add("No specific event (general attendance)");
                    ids.add(null);
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String t = doc.getString("title");
                        if (t != null) {
                            titles.add(t);
                            ids.add(doc.getId());
                        }
                    }
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Select Event")
                            .setItems(titles.toArray(new String[0]), (dialog, which) -> {
                                selectedEventId = ids.get(which);
                                selectedEventTitle = which == 0 ? null : titles.get(which);
                                if (selectedEventTitle != null) {
                                    btnSelectEvent.setText("Event: " + selectedEventTitle);
                                } else {
                                    btnSelectEvent.setText("General Attendance");
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show());
    }

    // ── Lifecycle ───────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeScanner != null && barcodeScanner.getVisibility() == View.VISIBLE) {
            barcodeScanner.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScanner != null) {
            barcodeScanner.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
