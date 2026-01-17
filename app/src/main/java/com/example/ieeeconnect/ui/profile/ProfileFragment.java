package com.example.ieeeconnect.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.activities.EditProfileActivity;
import com.example.ieeeconnect.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private Bitmap qrBitmap = null;
    private static final String TAG = "ProfileFragment";

    private final ActivityResultLauncher<Intent> pickProfileImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadImage(uri, "profile");
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickCoverImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadImage(uri, "cover");
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup listeners
        binding.btnChangePhoto.setOnClickListener(v -> pickImage("profile"));
        binding.btnChangeCover.setOnClickListener(v -> pickImage("cover"));
        
        binding.btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });
        
        binding.btnShareProfile.setOnClickListener(v -> showShareProfileDialog());

        loadUserData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning from EditProfileActivity
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in");
            return;
        }
        
        String uid = currentUser.getUid();
        String email = currentUser.getEmail();
        String authDisplayName = currentUser.getDisplayName();

        // 1. Immediate UI update with fallbacks from Auth/Email
        String emailPart = (email != null && email.contains("@")) ? email.split("@")[0] : "User";
        String initialName = !TextUtils.isEmpty(authDisplayName) ? authDisplayName : 
                            emailPart.substring(0, 1).toUpperCase() + emailPart.substring(1);
        String initialHandle = "@" + emailPart.toLowerCase();

        // Set initial values
        binding.profileName.setText(initialName);
        binding.profileUsername.setText(initialHandle);
        binding.profileEmail.setText(email != null ? email : "");

        generateAndSetQr(uid);

        // 2. Refresh with Database Data
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null || !isAdded()) return;
                    
                    if (doc != null && doc.exists()) {
                        String dbDisplayName = doc.getString("displayName");
                        if (TextUtils.isEmpty(dbDisplayName)) dbDisplayName = doc.getString("name");
                        String dbUsername = doc.getString("username");
                        String photoUrl = doc.getString("photoUrl");
                        String coverUrl = doc.getString("coverUrl");
                        String role = doc.getString("role");

                        // Apply DB Name if available
                        if (!TextUtils.isEmpty(dbDisplayName)) {
                            binding.profileName.setText(dbDisplayName);
                        }

                        // Apply DB Username if available
                        if (!TextUtils.isEmpty(dbUsername)) {
                            String handle = dbUsername.startsWith("@") ? dbUsername : "@" + dbUsername;
                            binding.profileUsername.setText(handle.toLowerCase());
                        }

                        // Load Images
                        if (!TextUtils.isEmpty(photoUrl)) {
                            Glide.with(this).load(photoUrl)
                                 .placeholder(R.drawable.ic_profile_placeholder)
                                 .into(binding.profileImage);
                        }
                        if (!TextUtils.isEmpty(coverUrl)) {
                            Glide.with(this).load(coverUrl)
                                 .placeholder(R.drawable.cover_placeholder)
                                 .into(binding.coverImage);
                        }
                        
                        if (!TextUtils.isEmpty(role)) {
                            binding.statCommitteeRole.setText(role);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firestore data load failed", e));
    }

    private void pickImage(String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if ("profile".equals(type)) {
            pickProfileImageLauncher.launch(Intent.createChooser(intent, "Select Profile Image"));
        } else {
            pickCoverImageLauncher.launch(Intent.createChooser(intent, "Select Cover Image"));
        }
    }

    private void uploadImage(Uri uri, String type) {
        try {
            Bitmap original = getBitmapFromUri(uri);
            if (original == null) return;

            Bitmap resized = scaleDownBitmap(original, 1024);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] data = baos.toByteArray();

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String path = "profiles/" + uid + "_" + type + ".jpg";
            StorageReference ref = storage.getReference().child(path);

            ref.putBytes(data)
                    .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        if (binding == null || !isAdded()) return;

                        String url = downloadUri.toString();
                        Map<String, Object> update = new HashMap<>();
                        if ("profile".equals(type)) {
                            update.put("photoUrl", url);
                            Glide.with(this).load(url).into(binding.profileImage);
                        } else {
                            update.put("coverUrl", url);
                            Glide.with(this).load(url).into(binding.coverImage);
                        }

                        firestore.collection("users").document(uid).update(update)
                                .addOnFailureListener(e -> firestore.collection("users").document(uid).set(update, com.google.firebase.firestore.SetOptions.merge()));
                        
                        Toast.makeText(getContext(), type + " image updated", Toast.LENGTH_SHORT).show();
                    }))
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
        }
    }

    private void generateAndSetQr(String text) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder encoder = new BarcodeEncoder();
            qrBitmap = encoder.createBitmap(matrix);
        } catch (Exception e) {
            Log.e(TAG, "QR Generation failed", e);
        }
    }

    private void showShareProfileDialog() {
        if (qrBitmap == null) {
            Toast.makeText(getContext(), "QR not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_share_profile, null);
        ImageView qrImg = dialogView.findViewById(R.id.dialog_qr_image);
        TextView emailTxt = dialogView.findViewById(R.id.dialog_qr_email);
        View closeBtn = dialogView.findViewById(R.id.btn_close_dialog);

        qrImg.setImageBitmap(qrBitmap);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            emailTxt.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.TransparentDialog)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(in);
        }
    }

    private Bitmap scaleDownBitmap(Bitmap realImage, int maxImageSize) {
        float ratio = Math.min((float) maxImageSize / realImage.getWidth(), (float) maxImageSize / realImage.getHeight());
        if (ratio >= 1.0f) return realImage;
        int width = Math.round(ratio * realImage.getWidth());
        int height = Math.round(ratio * realImage.getHeight());
        return Bitmap.createScaledBitmap(realImage, Math.max(1, width), Math.max(1, height), true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
