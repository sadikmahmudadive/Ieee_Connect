package com.example.ieeeconnect.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.databinding.ActivityEditProfileBinding;
import com.example.ieeeconnect.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private String uid;
    private String currentPhotoUrl;
    private boolean isImageChanged = false;
    private byte[] pendingImageData = null;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            Bitmap original = getBitmapFromUri(uri);
                            if (original != null) {
                                Bitmap resized = scaleDownBitmap(original, 800);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                resized.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                                pendingImageData = baos.toByteArray();
                                binding.editProfileImage.setImageBitmap(resized);
                                isImageChanged = true;
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadCurrentUserData();

        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnDone.setOnClickListener(v -> saveProfileChanges());
        
        View.OnClickListener pickImageClick = v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickImageLauncher.launch(Intent.createChooser(intent, "Select Profile Photo"));
        };
        binding.btnChangePhotoText.setOnClickListener(pickImageClick);
        binding.editProfileImage.setOnClickListener(pickImageClick);
    }

    private void loadCurrentUserData() {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Robust name loading
                        String name = doc.getString("displayName");
                        if (TextUtils.isEmpty(name)) name = doc.getString("name");
                        binding.etName.setText(name);

                        binding.etUsername.setText(doc.getString("username"));
                        binding.etRole.setText(doc.getString("role"));
                        binding.etDept.setText(doc.getString("dept"));
                        binding.etEmail.setText(doc.getString("email"));
                        binding.etPhone.setText(doc.getString("phone"));
                        binding.etGender.setText(doc.getString("gender"));
                        
                        currentPhotoUrl = doc.getString("photoUrl");
                        if (!TextUtils.isEmpty(currentPhotoUrl)) {
                            Glide.with(this).load(currentPhotoUrl).placeholder(R.drawable.ic_profile_placeholder).into(binding.editProfileImage);
                        }
                    }
                });
    }

    private void saveProfileChanges() {
        binding.btnDone.setEnabled(false);
        String name = binding.etName.getText().toString().trim();
        String dept = binding.etDept.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String gender = binding.etGender.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required");
            binding.btnDone.setEnabled(true);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", name);
        updates.put("dept", dept);
        updates.put("phone", phone);
        updates.put("gender", gender);
        // Note: username, role, and email are read-only and explicitly excluded from updates

        if (isImageChanged && pendingImageData != null) {
            uploadImageAndSave(updates);
        } else {
            saveToFirestore(updates);
        }
    }

    private void uploadImageAndSave(Map<String, Object> updates) {
        StorageReference ref = storage.getReference().child("profiles/" + uid + "_profile.jpg");
        ref.putBytes(pendingImageData)
                .addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    updates.put("photoUrl", uri.toString());
                    saveToFirestore(updates);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    saveToFirestore(updates);
                });
    }

    private void saveToFirestore(Map<String, Object> updates) {
        firestore.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    firestore.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e2 -> {
                                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                                binding.btnDone.setEnabled(true);
                            });
                });
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
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
}
