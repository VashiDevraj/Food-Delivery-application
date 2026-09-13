package com.example.fooddeliveryapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.activities.AdminDashboardActivity;
import com.example.fooddeliveryapp.activities.RegisterActivity;
import com.example.fooddeliveryapp.activities.RestaurantSetupActivity;
import com.example.fooddeliveryapp.databinding.FragmentAdminAuthBinding;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminAuthFragment extends Fragment {

    private FragmentAdminAuthBinding binding;

    private FirebaseAuth       firebaseAuth;
    private DatabaseReference  dbRef;
    private GoogleSignInClient googleSignInClient;
    private SessionManager     sessionManager;

    private static final int RC_GOOGLE_SIGN_IN = 202;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminAuthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth   = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(requireContext());
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        binding.btnLogin.setOnClickListener(v -> loginWithEmail());
        binding.btnGoogleAdmin.setOnClickListener(v ->
                startActivityForResult(googleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN));
        binding.tvSignup.setOnClickListener(v -> {
            Intent i = new Intent(requireActivity(), RegisterActivity.class);
            i.putExtra("role", Constants.ROLE_ADMIN);
            startActivity(i);
        });
        binding.tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ── Email Login ───────────────────────────────────────────────────────────

    private void loginWithEmail() {
        if (binding == null) return;

        String email    = binding.etEmail.getText()    != null ? binding.etEmail.getText().toString().trim()    : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email address");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        setLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!isAdded() || binding == null) return;
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        verifyAdminInDatabase(firebaseAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "Login failed";
                        showToast("Login failed: " + msg);
                    }
                });
    }

    // ── Password Reset ────────────────────────────────────────────────────────

    private void sendPasswordReset() {
        if (binding == null) return;
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Enter your email to reset password");
            return;
        }
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(u -> showToast("Reset link sent to " + email))
                .addOnFailureListener(e -> showToast("Failed: " + e.getMessage()));
    }

    // ── Verify admin node ─────────────────────────────────────────────────────

    private void verifyAdminInDatabase(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        dbRef.child(Constants.NODE_ADMINS).child(uid).child(Constants.NODE_PROFILE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || binding == null) return;
                        setLoading(false);

                        if (!snapshot.exists()) {
                            firebaseAuth.signOut();
                            showToast("No admin account found. Please register as admin first.");
                            return;
                        }

                        String name = snapshot.child("name").getValue(String.class);
                        if (name == null || name.isEmpty()) name = firebaseUser.getEmail();

                        sessionManager.saveSession(uid, name, Constants.ROLE_ADMIN);
                        cacheAdminProfileToSession(snapshot);

                        // Route based on Firebase, not local flag
                        routeAdminViaFirebase(uid);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded() || binding == null) return;
                        setLoading(false);
                        showToast("Database error: " + error.getMessage());
                    }
                });
    }

    // ── Google Login ──────────────────────────────────────────────────────────

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_GOOGLE_SIGN_IN) return;

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                setLoading(true);
                authenticateWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            showToast("Google Sign-In failed. Please try again.");
        }
    }

    private void authenticateWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (!isAdded() || binding == null) return;
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        verifyAdminInDatabase(firebaseAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        showToast("Google authentication failed.");
                    }
                });
    }

    // ── Route admin — checks Firebase for restaurant info ─────────────────────

    /**
     * Instead of trusting the local SharedPreferences flag (which is never
     * set on a fresh install / new device), we query Firebase directly.
     *
     * If Admins/{uid}/Restaurant/info/name exists  → setup is done → Dashboard
     * Otherwise                                    → setup needed  → SetupActivity
     */
    private void routeAdminViaFirebase(String uid) {
        if (!isAdded() || binding == null) return;
        setLoading(true);

        dbRef.child(Constants.NODE_ADMINS).child(uid)
                .child("Restaurant").child("info")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || binding == null) return;
                        setLoading(false);

                        String restaurantName = snapshot.child("name").getValue(String.class);
                        boolean setupDone = snapshot.exists()
                                && restaurantName != null
                                && !restaurantName.isEmpty();

                        if (setupDone) {
                            // Mark locally so future checks are instant
                            sessionManager.setRestaurantSetupDone();
                            navigateTo(AdminDashboardActivity.class);
                        } else {
                            navigateTo(RestaurantSetupActivity.class);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded() || binding == null) return;
                        setLoading(false);
                        // On error, fall back to local flag
                        if (sessionManager.isRestaurantSetupDone()) {
                            navigateTo(AdminDashboardActivity.class);
                        } else {
                            navigateTo(RestaurantSetupActivity.class);
                        }
                    }
                });
    }

    // ── Cache profile fields into SessionManager ──────────────────────────────

    private void cacheAdminProfileToSession(DataSnapshot snapshot) {
        String phone = snapshot.child("phone").getValue(String.class);
        if (phone != null && !phone.isEmpty()) {
            sessionManager.saveUserPhone(phone);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void navigateTo(Class<?> destination) {
        if (!isAdded() || getActivity() == null || binding == null) return;
        Intent i = new Intent(requireActivity(), destination);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        requireActivity().finish();
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogleAdmin.setEnabled(!loading);
    }

    private void showToast(String msg) {
        if (getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }
}