package com.example.ieeeconnect.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
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

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private Bitmap qrBitmap = null;
    private static final String TAG = "ProfileFragment";

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadProfileImage(uri);
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

        // Edit and camera badge both open image picker
        View.OnClickListener pickImageClick = v -> pickImageFromGallery();
        binding.btnEditProfile.setOnClickListener(pickImageClick);
        // btn_change_photo maps to binding.btnChangePhoto via data binding
        try {
            binding.btnChangePhoto.setOnClickListener(pickImageClick);
        } catch (Exception ignored) {}

        // load basic user info
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            binding.profileName.setText(name == null ? "No name" : name);

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            generateAndSetQr(uid);

            // set QR email text
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            if (email != null) {
                try { binding.qrEmail.setText(email); } catch (Exception ignored) {}
            }


            // Load user's saved profile photo from Firestore if present; fallback to Auth photo
            firestore.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            Object photoObj = doc.get("photoUrl");
                            if (photoObj instanceof String) {
                                String photoUrl = (String) photoObj;
                                if (!photoUrl.isEmpty()) {
                                    Glide.with(requireContext()).load(photoUrl).into(binding.profileImage);
                                    return;
                                }
                            }
                        }
                        // fallback to FirebaseUser photo URL
                        if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null) {
                            Glide.with(requireContext()).load(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl()).into(binding.profileImage);
                        } else {
                            // leave placeholder (defined in layout)
                            Log.d(TAG, "No profile image available for user: " + uid);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to read user document", e);
                        if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null) {
                            Glide.with(requireContext()).load(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl()).into(binding.profileImage);
                        }
                    });

            // Clicking profile image shows fullscreen preview (if available)
            binding.profileImage.setOnClickListener(v -> {
                ImageView iv = new ImageView(requireContext());
                iv.setAdjustViewBounds(true);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

                try {
                    String uidLocal = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                    if (uidLocal != null) {
                        firestore.collection("users").document(uidLocal).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc != null && doc.exists()) {
                                        Object photoObj = doc.get("photoUrl");
                                        if (photoObj instanceof String && !((String) photoObj).isEmpty()) {
                                            Glide.with(requireContext()).load((String) photoObj).into(iv);
                                        } else if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null) {
                                            Glide.with(requireContext()).load(FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl()).into(iv);
                                        } else {
                                            iv.setImageDrawable(binding.profileImage.getDrawable());
                                        }
                                        AlertDialog dlg = new AlertDialog.Builder(requireContext()).setView(iv).setPositiveButton("Close", (d, w) -> d.dismiss()).create();
                                        dlg.show();
                                    } else {
                                        iv.setImageDrawable(binding.profileImage.getDrawable());
                                        AlertDialog dlg = new AlertDialog.Builder(requireContext()).setView(iv).setPositiveButton("Close", (d, w) -> d.dismiss()).create();
                                        dlg.show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    iv.setImageDrawable(binding.profileImage.getDrawable());
                                    AlertDialog dlg = new AlertDialog.Builder(requireContext()).setView(iv).setPositiveButton("Close", (d, w) -> d.dismiss()).create();
                                    dlg.show();
                                });
                    } else {
                        iv.setImageDrawable(binding.profileImage.getDrawable());
                        AlertDialog dlg = new AlertDialog.Builder(requireContext()).setView(iv).setPositiveButton("Close", (d, w) -> d.dismiss()).create();
                        dlg.show();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error showing fullscreen profile image", ex);
                    Toast.makeText(requireContext(), "Unable to show image", Toast.LENGTH_SHORT).show();
                }
            });
        }

        binding.btnShowQrFullscreen.setOnClickListener(v -> showQrFullscreen());
    }

    private void pickImageFromGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(i);
    }

    // Generate QR and set to image; keep a reference to the Bitmap
    private void generateAndSetQr(String text) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bmp = encoder.createBitmap(matrix);
            qrBitmap = bmp;
            binding.qrImage.setImageBitmap(bmp);
        } catch (Exception e) {
            Log.e(TAG, "generateAndSetQr: failure", e);
            Log.w(TAG, "generateAndSetQr exception: " + e.getMessage());
        }
    }

    // Show the QR in a simple fullscreen dialog
    private void showQrFullscreen() {
        if (qrBitmap == null) {
            Toast.makeText(requireContext(), "QR not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageView iv = new ImageView(requireContext());
        iv.setImageBitmap(qrBitmap);
        iv.setAdjustViewBounds(true);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(iv)
                .setPositiveButton("Close", (d, w) -> d.dismiss())
                .create();
        dialog.show();
    }

    // Upload with simple compression/resizing before upload
    private void uploadProfileImage(Uri uri) {
        try {
            Bitmap original = getBitmapFromUri(uri);
            if (original == null) {
                Toast.makeText(requireContext(), "Unable to read image", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap resized = scaleDownBitmap(original, 800); // max dim 800px
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] data = baos.toByteArray();

            String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown";
            StorageReference ref = storage.getReference().child("profiles/" + uid + ".jpg");

            ref.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        // Save URL to Firestore and update UI.
                        try {
                            firestore.collection("users").document(uid)
                                    .update("photoUrl", downloadUri.toString())
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "photoUrl updated in Firestore"))
                                    .addOnFailureListener(e -> {
                                        // If update fails (e.g., doc doesn't exist), set the document instead
                                        firestore.collection("users").document(uid)
                                                .set(new java.util.HashMap<String, Object>() {{ put("photoUrl", downloadUri.toString()); }})
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "photoUrl set in Firestore"))
                                                .addOnFailureListener(e2 -> Log.e(TAG, "Failed to save photoUrl", e2));
                                    });

                            // Show immediately
                            Glide.with(binding.profileImage.getContext()).load(downloadUri).into(binding.profileImage);
                            Toast.makeText(requireContext(), "Profile image uploaded", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "uploadProfileImage:onSuccess error", e);
                            Toast.makeText(requireContext(), "Upload succeeded but saving failed", Toast.LENGTH_LONG).show();
                        }
                    }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "uploadProfileImage:failure", e);
                        Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            Log.e(TAG, "uploadProfileImage:exception", e);
            Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Helper to load a Bitmap from Uri safely
    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        InputStream in = null;
        try {
            in = requireContext().getContentResolver().openInputStream(uri);
            if (in == null) return null;
            return BitmapFactory.decodeStream(in);
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
        }
    }

    // Scale down bitmap while keeping aspect ratio
    private Bitmap scaleDownBitmap(Bitmap realImage, int maxImageSize) {
        float ratio = Math.min((float) maxImageSize / realImage.getWidth(), (float) maxImageSize / realImage.getHeight());
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
