package com.example.ieeeconnect;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    private static final String PREFS_NAME = "ieee_prefs";
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // If already signed in, check admin role and navigate accordingly
            checkAdminAndNavigate();
            return;
        }
        if (!hasSeenOnboarding()) {
            Intent intent = new Intent(this, com.example.ieeeconnect.ui.onboarding.OnboardingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupGoogleSignIn();
        setupGoogleLauncher();


        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailInput.getText() != null ? binding.emailInput.getText().toString().trim() : "";
            String password = binding.passwordInput.getText() != null ? binding.passwordInput.getText().toString() : "";
            if (email.isEmpty() || password.isEmpty()) {
                toast("Email and password required");
                return;
            }
            signIn(email, password);
        });

        binding.signupLink.setOnClickListener(v -> {
            String email = binding.emailInput.getText() != null ? binding.emailInput.getText().toString().trim() : "";
            if (email.isEmpty()) {
                toast("Please enter your email to sign up.");
                return;
            }
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            intent.putExtra("email", email);
            startActivity(intent);
        });

        binding.googleButton.setOnClickListener(v -> {
            Intent intent = googleSignInClient.getSignInIntent();
            googleLauncher.launch(intent);
        });

        binding.btnFacebook.setOnClickListener(v -> toast("Facebook sign-in not implemented"));
        binding.btnApple.setOnClickListener(v -> toast("Apple sign-in not implemented"));

        binding.forgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private void handleForgotPassword() {
        String email = binding.emailInput.getText() != null ? binding.emailInput.getText().toString().trim() : "";
        if (email.isEmpty()) {
            binding.emailLayout.setError("This field is required");
            return;
        }
        binding.emailLayout.setError(null);

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> showInstructionDialog())
                .addOnFailureListener(e -> toast("Failed to send reset email: " + e.getMessage()));
    }

    private void showInstructionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_instruction_sent, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        Button okayButton = dialogView.findViewById(R.id.okay_button);
        okayButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private boolean hasSeenOnboarding() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_ONBOARDING_SEEN, false);
    }

    private void signIn(String email, String password) {
        binding.loginButton.setEnabled(false);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    // After successful sign-in, perform admin check and navigate accordingly
                    checkAdminAndNavigate();
                })
                .addOnFailureListener(e -> {
                    binding.loginButton.setEnabled(true);
                    toast("Login failed: " + e.getMessage());
                });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupGoogleLauncher() {
        googleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    toast("Google sign-in failed (no token)");
                }
            } catch (ApiException e) {
                toast("Google sign-in failed");
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        binding.loginButton.setEnabled(false);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    // After successful Google sign-in, perform admin check and navigate accordingly
                    checkAdminAndNavigate();
                })
                .addOnFailureListener(e -> {
                    binding.loginButton.setEnabled(true);
                    toast("Google auth failed: " + e.getMessage());
                });
    }

    // New helper: fetch users/{uid} and navigate to admin or normal dashboard
    private void checkAdminAndNavigate() {
        if (auth.getCurrentUser() == null) {
            navigateToDashboard();
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    android.util.Log.d("MainActivity", "User doc: " + documentSnapshot.getData());
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
                    } else {
                        android.util.Log.e("MainActivity", "User document does not exist for UID: " + userId);
                    }
                    android.util.Log.d("MainActivity", "isAdmin: " + isAdmin + ", role: " + role);

                    boolean isAdminRole = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("SUPER_ADMIN");
                    if (isAdmin || isAdminRole) {
                        android.util.Log.i("MainActivity", "Navigating to AdminDashboard via DashboardActivity");
                        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                        intent.putExtra("isAdmin", true);
                        intent.putExtra("role", role);
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException ex) {
                            android.util.Log.e("MainActivity", "Failed to start DashboardActivity: " + ex.getMessage());
                            navigateToDashboard();
                            return;
                        }
                        finish();
                    } else {
                        navigateToDashboard();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MainActivity", "Failed to fetch user info: " + e.getMessage());
                    // fallback to normal dashboard
                    navigateToDashboard();
                });
    }

    private void navigateToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
