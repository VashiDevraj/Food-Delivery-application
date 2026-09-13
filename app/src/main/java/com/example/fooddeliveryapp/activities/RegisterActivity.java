package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.databinding.ActivityRegisterBinding;
import com.example.fooddeliveryapp.models.Admin;
import com.example.fooddeliveryapp.models.User;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth            firebaseAuth;
    private DatabaseReference       dbRef;
    private SessionManager          sessionManager;
    private GoogleSignInClient      googleSignInClient;
    private String                  registrationRole;

    private static final int RC_GOOGLE = 301;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding        = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth   = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        registrationRole = getIntent().getStringExtra("role");
        if (registrationRole == null || registrationRole.isEmpty()) {
            registrationRole = Constants.ROLE_USER;
        }

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnRegister.setOnClickListener(v -> attemptRegister());

        binding.btnGoogleRegister.setOnClickListener(v -> {
            showLoading(true);
            startActivityForResult(googleSignInClient.getSignInIntent(), RC_GOOGLE);
        });

        binding.tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
        });
    }

    // ── EMAIL REGISTER ─────────────────────────────────────────────────────────

    private void attemptRegister() {
        String name     = binding.etName.getText().toString().trim();
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirm  = binding.etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name))   { binding.etName.setError("Full name is required"); return; }
        if (TextUtils.isEmpty(email))  { binding.etEmail.setError("Email is required"); return; }
        if (password.length() < 6)     { binding.etPassword.setError("Min 6 characters"); return; }
        if (!password.equals(confirm)) { binding.etConfirmPassword.setError("Passwords do not match"); return; }

        showLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        saveProfile(firebaseAuth.getCurrentUser().getUid(), name, email);
                    } else {
                        showLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ── GOOGLE REGISTER ────────────────────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                showLoading(false);
                Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        String name  = user.getDisplayName() != null ? user.getDisplayName() : "User";
                        String email = user.getEmail() != null ? user.getEmail() : "";
                        saveProfile(user.getUid(), name, email);
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Google Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── SAVE PROFILE ───────────────────────────────────────────────────────────

    private void saveProfile(String uid, String name, String email) {
        DatabaseReference profileRef;
        Object            profileObj;

        if (Constants.ROLE_ADMIN.equals(registrationRole)) {
            Admin admin = new Admin(uid, name, email);
            profileRef  = dbRef.child(Constants.NODE_ADMINS).child(uid).child(Constants.NODE_PROFILE);
            profileObj  = admin;
        } else {
            User user  = new User(uid, name, email, Constants.ROLE_USER);
            profileRef = dbRef.child(Constants.NODE_USERS).child(uid).child(Constants.NODE_PROFILE);
            profileObj = user;
        }

        profileRef.setValue(profileObj)
                .addOnSuccessListener(unused -> {
                    showLoading(false);
                    sessionManager.saveSession(uid, name, registrationRole);

                    if (Constants.ROLE_ADMIN.equals(registrationRole)) {
                        startActivity(new Intent(this, RestaurantSetupActivity.class));
                    } else {
                        startActivity(new Intent(this, UserDashboardActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
        if (binding.btnGoogleRegister != null) binding.btnGoogleRegister.setEnabled(!loading);
    }
}