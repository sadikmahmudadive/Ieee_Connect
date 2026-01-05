package com.example.ieeeconnect;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ieeeconnect.activities.SignUpActivity;
import com.example.ieeeconnect.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    // Google Sign-In
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null && account.getIdToken() != null) {
                        firebaseAuthWithGoogle(account.getIdToken());
                    } else {
                        Toast.makeText(this, "Google sign-in failed (no token)", Toast.LENGTH_LONG).show();
                    }
                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                // user cancelled or error
            }
        });

        // Wire login button
        binding.loginButton.setOnClickListener(v -> onLoginClicked());

        // Social buttons (ImageButton ids exist in layout)
        ImageButton fb = binding.btnFacebook;
        ImageButton apple = binding.btnApple;
        ImageButton google = binding.googleButton;

        fb.setOnClickListener(v -> Toast.makeText(this, "Facebook sign-in not implemented", Toast.LENGTH_SHORT).show());
        apple.setOnClickListener(v -> Toast.makeText(this, "Apple sign-in not implemented", Toast.LENGTH_SHORT).show());
        google.setOnClickListener(v -> onGoogleSignInClicked());

        // Signup link
        binding.signupLink.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));

        binding.forgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void onLoginClicked() {
        String email = binding.emailInput.getText() == null ? null : binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText() == null ? null : binding.passwordInput.getText().toString();

        if (TextUtils.isEmpty(email)) {
            binding.emailInput.setError(getString(R.string.error_email_required));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.passwordInput.setError(getString(R.string.error_password_required));
            return;
        }

        binding.loginButton.setEnabled(false);
        binding.loginButton.setText(R.string.signing_in);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.loginButton.setEnabled(true);
                    binding.loginButton.setText(R.string.log_in);
                    if (task.isSuccessful()) {
                        // Check Firestore for admin role and role field
                        String userId = auth.getCurrentUser().getUid();
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    android.util.Log.d("LoginActivity", "User doc: " + documentSnapshot.getData());
                                    boolean isAdmin = false;
                                    String role = "";
                                    if (documentSnapshot.exists()) {
                                        Object isAdminObj = documentSnapshot.get("isAdmin");
                                        Object roleObj = documentSnapshot.get("role");
                                        // Defensive type checking for isAdmin
                                        if (isAdminObj instanceof Boolean) {
                                            isAdmin = (Boolean) isAdminObj;
                                        } else if (isAdminObj instanceof String) {
                                            String val = ((String) isAdminObj).trim().toLowerCase();
                                            isAdmin = val.equals("true") || val.equals("1") || val.equals("yes");
                                        } else if (isAdminObj != null) {
                                            String val = isAdminObj.toString().trim().toLowerCase();
                                            isAdmin = val.equals("true") || val.equals("1") || val.equals("yes");
                                        }
                                        // Defensive type checking for role
                                        if (roleObj != null) {
                                            role = roleObj.toString().trim();
                                        }
                                    } else {
                                        android.util.Log.e("LoginActivity", "User document does not exist for UID: " + userId);
                                    }
                                    android.util.Log.d("LoginActivity", "isAdmin: " + isAdmin + ", role: " + role);
                                    Toast.makeText(LoginActivity.this, "isAdmin: " + isAdmin + ", role: " + role, Toast.LENGTH_LONG).show();

                                    // Defensive navigation
                                    boolean isAdminRole = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("SUPER_ADMIN");
                                    if (isAdmin || isAdminRole) {
                                        android.util.Log.i("LoginActivity", "Navigating to AdminDashboard via DashboardActivity");
                                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                        intent.putExtra("isAdmin", true); // Pass admin flag for extra safety
                                        intent.putExtra("role", role);
                                        try {
                                            startActivity(intent);
                                        } catch (ActivityNotFoundException ex) {
                                            android.util.Log.e("LoginActivity", "Failed to start DashboardActivity: " + ex.getMessage());
                                            // Fallback: start DashboardActivity without extras
                                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                        }
                                        finish();
                                    } else {
                                        android.util.Log.i("LoginActivity", "Navigating to DashboardActivity");
                                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("LoginActivity", "Failed to fetch user info: " + e.getMessage());
                                    Toast.makeText(LoginActivity.this, "Failed to fetch user info: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    // Fallback: go to normal dashboard
                                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                    finish();
                                });
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void onGoogleSignInClicked() {
        binding.loginButton.setEnabled(false);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
        binding.loginButton.setEnabled(true);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        binding.loginButton.setEnabled(false);
        binding.loginButton.setText(R.string.signing_in);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            binding.loginButton.setEnabled(true);
            binding.loginButton.setText(R.string.log_in);
            if (task.isSuccessful()) {
                // Check Firestore for admin role and role field (same as email/password login)
                String userId = auth.getCurrentUser().getUid();
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            android.util.Log.d("LoginActivity", "User doc: " + documentSnapshot.getData());
                            boolean isAdmin = false;
                            String role = "";
                            if (documentSnapshot.exists()) {
                                Object isAdminObj = documentSnapshot.get("isAdmin");
                                Object roleObj = documentSnapshot.get("role");
                                if (isAdminObj instanceof Boolean) {
                                    isAdmin = (Boolean) isAdminObj;
                                } else if (isAdminObj instanceof String) {
                                    String val = ((String) isAdminObj).trim().toLowerCase();
                                    isAdmin = val.equals("true") || val.equals("1") || val.equals("yes");
                                } else if (isAdminObj != null) {
                                    String val = isAdminObj.toString().trim().toLowerCase();
                                    isAdmin = val.equals("true") || val.equals("1") || val.equals("yes");
                                }
                                if (roleObj != null) {
                                    role = roleObj.toString().trim();
                                }
                            }
                            Toast.makeText(LoginActivity.this, "isAdmin: " + isAdmin + ", role: " + role, Toast.LENGTH_LONG).show();
                            boolean isAdminRole = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("SUPER_ADMIN");
                            if (isAdmin || isAdminRole) {
                                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                                intent.putExtra("isAdmin", true);
                                intent.putExtra("role", role);
                                try {
                                    startActivity(intent);
                                } catch (ActivityNotFoundException ex) {
                                    android.util.Log.e("LoginActivity", "Failed to start DashboardActivity: " + ex.getMessage());
                                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                }
                                finish();
                            } else {
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(LoginActivity.this, "Failed to fetch user info: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        });
            } else {
                Toast.makeText(LoginActivity.this, "Google authentication failed: " + (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
            }
        });
    }
}
