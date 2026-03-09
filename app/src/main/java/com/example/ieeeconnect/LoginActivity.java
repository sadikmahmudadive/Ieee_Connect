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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

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
            }
        });

        binding.loginButton.setOnClickListener(v -> onLoginClicked());

        binding.googleButton.setOnClickListener(v -> onGoogleSignInClicked());
        binding.signupLink.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));
        binding.forgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void onLoginClicked() {
        String email = binding.emailInput.getText() == null ? null : binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText() == null ? null : binding.passwordInput.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loginButton.setEnabled(false);
        binding.loginButton.setText(R.string.signing_in);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveFcmTokenAndNavigate();
                    } else {
                        binding.loginButton.setEnabled(true);
                        binding.loginButton.setText(R.string.log_in);
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void onGoogleSignInClicked() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                saveFcmTokenAndNavigate();
            } else {
                Toast.makeText(LoginActivity.this, "Google authentication failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveFcmTokenAndNavigate() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Fetch and save the FCM token for notifications
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String token = task.isSuccessful() ? task.getResult() : null;
            if (token != null) {
                FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                        .update("fcmToken", token);
            }
            
            // Navigate to Dashboard
            checkUserRoleAndNavigate(user.getUid());
        });
    }

    private void checkUserRoleAndNavigate(String userId) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean isAdmin = false;
                    String role = "";
                    if (documentSnapshot.exists()) {
                        isAdmin = documentSnapshot.getBoolean("isAdmin") != null && documentSnapshot.getBoolean("isAdmin");
                        role = documentSnapshot.getString("role");
                    }
                    
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.putExtra("isAdmin", isAdmin || "ADMIN".equalsIgnoreCase(role));
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                });
    }
}
