package com.example.ieeeconnect.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.LoginActivity;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.activities.EditProfileActivity;
import com.example.ieeeconnect.databinding.FragmentProfileBinding;
import com.example.ieeeconnect.domain.model.Event;
import com.example.ieeeconnect.viewmodels.EventsViewModel;
import com.example.ieeeconnect.data.remote.CloudinaryManager;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private EventsViewModel eventsViewModel;
    private MyEventsAdapter myEventsAdapter;

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

        // Setup RecyclerView
        myEventsAdapter = new MyEventsAdapter();
        binding.myEventsRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.myEventsRecycler.setAdapter(myEventsAdapter);

        // Setup ViewModel
        eventsViewModel = new ViewModelProvider(this).get(EventsViewModel.class);

        // Setup listeners
        binding.btnChangePhoto.setOnClickListener(v -> pickImage("profile"));
        binding.btnChangeCover.setOnClickListener(v -> pickImage("cover"));

        binding.btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        binding.btnShareProfile.setOnClickListener(v -> showShareProfileDialog());

        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        loadUserData();
        observeEvents();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to the fragment
        loadUserData();
        if (eventsViewModel != null) {
            eventsViewModel.refreshFromNetwork();
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        String email = currentUser.getEmail();
        String authDisplayName = currentUser.getDisplayName();

        // Calculate Join Year from metadata
        if (currentUser.getMetadata() != null) {
            long created = currentUser.getMetadata().getCreationTimestamp();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(created);
            binding.statMemberSince.setText(String.valueOf(cal.get(Calendar.YEAR)));
        }

        // Initial UI update with fallbacks
        String emailPart = (email != null && email.contains("@")) ? email.split("@")[0] : "User";
        String initialName = !TextUtils.isEmpty(authDisplayName) ? authDisplayName :
                            emailPart.substring(0, 1).toUpperCase() + emailPart.substring(1);

        binding.profileName.setText(initialName);
        binding.profileUsername.setText("@" + emailPart.toLowerCase());
        binding.profileEmail.setText(email != null ? email : "");

        generateAndSetQr(uid);

        // Load specific data from Firestore
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

                        if (!TextUtils.isEmpty(dbDisplayName)) {
                            binding.profileName.setText(dbDisplayName);
                        }

                        if (!TextUtils.isEmpty(dbUsername)) {
                            String handle = dbUsername.startsWith("@") ? dbUsername : "@" + dbUsername;
                            binding.profileUsername.setText(handle.toLowerCase());
                        }

                        if (!TextUtils.isEmpty(photoUrl)) {
                            Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileImage);
                        }
                        if (!TextUtils.isEmpty(coverUrl)) {
                            Glide.with(this).load(coverUrl).placeholder(R.drawable.cover_placeholder).into(binding.coverImage);
                        }

                        if (!TextUtils.isEmpty(role)) {
                            binding.statCommitteeRole.setText(role);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firestore data load failed", e));
    }

    private void observeEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        eventsViewModel.getAllEvents().observe(getViewLifecycleOwner(), allEvents -> {
            if (allEvents == null) return;

            List<Event> registeredEvents = new ArrayList<>();
            for (Event event : allEvents) {
                if (event.getGoingUserIds() != null && event.getGoingUserIds().contains(uid)) {
                    registeredEvents.add(event);
                }
            }

            myEventsAdapter.setEvents(registeredEvents);
            binding.statEventsAttended.setText(String.valueOf(registeredEvents.size()));
        });
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

            // Show preview dialog before upload
            showImagePreviewDialog(original, uri, type);
        } catch (IOException e) {
            Log.e(TAG, "Error processing image", e);
        }
    }

    private void showImagePreviewDialog(Bitmap bitmap, Uri uri, String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View previewView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_image_preview, null);
        ImageView previewImage = previewView.findViewById(R.id.preview_image);
        View btnUpload = previewView.findViewById(R.id.btn_upload_image);
        View btnCancel = previewView.findViewById(R.id.btn_cancel_upload);
        previewImage.setImageBitmap(bitmap);
        builder.setView(previewView);
        AlertDialog dialog = builder.create();
        btnUpload.setOnClickListener(v -> {
            dialog.dismiss();
            performImageUpload(uri, type);
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void performImageUpload(Uri uri, String type) {
        // Resize image locally and write to a temporary file, then upload that file
        try {
            Bitmap original = getBitmapFromUri(uri);
            if (original == null) return;

            // scale down to max 1024px (preserve aspect)
            Bitmap resized = scaleDownBitmap(original, 1024);

            // write resized to cache and get Uri
            Uri uploadUri = writeBitmapToCacheAndGetUri(resized, type.equals("profile") ? "profile_upload.jpg" : "cover_upload.jpg");
            if (uploadUri == null) {
                Toast.makeText(getContext(), "Failed to prepare image for upload", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress UI
            binding.uploadProgressBar.setVisibility(View.VISIBLE);
            binding.uploadProgressBar.setProgress(0);
            binding.uploadProgressText.setVisibility(View.VISIBLE);
            binding.uploadProgressText.setText("0%");
            binding.btnChangePhoto.setEnabled(false);
            binding.btnChangeCover.setEnabled(false);

            // Use CloudinaryManager to upload the image
            CloudinaryManager.upload(uploadUri, new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    // upload started
                    Log.d(TAG, "Cloudinary upload started: " + requestId);
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {
                    // update progress (Cloudinary progress gives bytes)
                    try {
                        int percent = (int) ((bytes * 100) / (totalBytes == 0 ? 1 : totalBytes));
                        if (binding != null) {
                            binding.uploadProgressBar.setIndeterminate(false);
                            binding.uploadProgressBar.setProgress(percent);
                            binding.uploadProgressText.setText(percent + "%");
                        }
                    } catch (Exception e) {
                        // ignore progress errors
                    }
                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    if (binding == null || !isAdded()) return;
                    Object secure = resultData != null ? resultData.get("secure_url") : null;
                    final String url = secure != null ? secure.toString() : (resultData != null ? String.valueOf(resultData.get("url")) : null);
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Map<String, Object> update = new HashMap<>();
                    if ("profile".equals(type)) {
                        update.put("photoUrl", url);
                        if (url != null) Glide.with(ProfileFragment.this).load(url).into(binding.profileImage);
                    } else {
                        update.put("coverUrl", url);
                        if (url != null) Glide.with(ProfileFragment.this).load(url).into(binding.coverImage);
                    }

                    if (url != null) {
                        firestore.collection("users").document(uid).update(update)
                                .addOnFailureListener(e -> firestore.collection("users").document(uid).set(update, com.google.firebase.firestore.SetOptions.merge()));
                        Toast.makeText(getContext(), type + " image updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Upload succeeded but no URL returned", Toast.LENGTH_SHORT).show();
                    }

                    binding.uploadProgressBar.setVisibility(View.GONE);
                    binding.uploadProgressText.setVisibility(View.GONE);
                    binding.btnChangePhoto.setEnabled(true);
                    binding.btnChangeCover.setEnabled(true);

                    // attempt to delete temp file
                    tryDeleteTempFile(uploadUri);
                }

                @Override
                public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                    if (binding == null || !isAdded()) return;
                    Log.e(TAG, "Cloudinary upload error: " + error.getDescription());
                    Toast.makeText(getContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    binding.uploadProgressBar.setVisibility(View.GONE);
                    binding.uploadProgressText.setVisibility(View.GONE);
                    binding.btnChangePhoto.setEnabled(true);
                    binding.btnChangeCover.setEnabled(true);

                    tryDeleteTempFile(uploadUri);
                }

                @Override
                public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                    // handle retries if needed
                    Log.w(TAG, "Cloudinary upload rescheduled: " + requestId + " -> " + (error != null ? error.getDescription() : ""));
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "Error preparing image for upload", e);
            Toast.makeText(getContext(), "Failed to prepare image", Toast.LENGTH_SHORT).show();
        }
    }

    private void tryDeleteTempFile(Uri uri) {
        try {
            if (uri == null) return;
            File f = new File(uri.getPath());
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}
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

    private Uri writeBitmapToCacheAndGetUri(Bitmap bmp, String filename) throws IOException {
        File cacheDir = requireContext().getCacheDir();
        File outFile = new File(cacheDir, filename);
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
        }
        return Uri.fromFile(outFile);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
