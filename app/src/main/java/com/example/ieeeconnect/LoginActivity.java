package com.example.ieeeconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ieeeconnect.activities.SignUpActivity;
import com.example.ieeeconnect.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
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
            binding.emailInput.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.passwordInput.setError("Password required");
            return;
        }

        binding.loginButton.setEnabled(false);
        binding.loginButton.setText("Signing in...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        binding.loginButton.setEnabled(true);
                        binding.loginButton.setText("Log In");
                        if (task.isSuccessful()) {
                            // Sign in success, go to Dashboard
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                        }
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
        binding.loginButton.setText("Signing in...");

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            binding.loginButton.setEnabled(true);
            binding.loginButton.setText("Log In");
            if (task.isSuccessful()) {
                // Sign in success
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Google authentication failed: " + (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
            }
        });
    }
}
