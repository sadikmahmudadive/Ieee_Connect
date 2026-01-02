package com.example.ieeeconnect;

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
            navigateToDashboard();
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
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> navigateToDashboard())
                .addOnFailureListener(e -> toast("Login failed: " + e.getMessage()));
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
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                toast("Google sign-in failed");
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> navigateToDashboard())
                .addOnFailureListener(e -> toast("Google auth failed: " + e.getMessage()));
    }

    private void navigateToDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
