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
import com.example.fooddeliveryapp.activities.RegisterActivity;
import com.example.fooddeliveryapp.activities.UserDashboardActivity;
import com.example.fooddeliveryapp.databinding.FragmentUserAuthBinding;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * UserAuthFragment — Email + Google login for customers.
 *
 * Key behaviours:
 *  - Email login: verifies user node exists in DB before creating session.
 *  - Google login: auto-creates profile on first sign-in.
 *  - After login: fetches full profile from Firebase and caches phone +
 *    address into SessionManager so downstream screens have it immediately.
 *  - ViewBinding used throughout (no findViewById scattered in code).
 */
public class UserAuthFragment extends Fragment {

    private FragmentUserAuthBinding binding;

    private FirebaseAuth       firebaseAuth;
    private DatabaseReference  dbRef;
    private GoogleSignInClient googleSignInClient;
    private SessionManager     sessionManager;

    private static final int RC_GOOGLE_SIGN_IN = 101;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserAuthBinding.inflate(inflater, container, false);
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
        binding.btnGoogle.setOnClickListener(v ->
                startActivityForResult(googleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN));
        binding.tvSignup.setOnClickListener(v -> {
            Intent i = new Intent(requireActivity(), RegisterActivity.class);
            i.putExtra("role", Constants.ROLE_USER);
            startActivity(i);
        });
        binding.tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // prevent memory leak
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

        // Clear previous errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        setLoading(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!isAdded() || binding == null) return;
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        verifyUserInDatabase(firebaseAuth.getCurrentUser());
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

    // ── Verify user node exists in Firebase ───────────────────────────────────

    /**
     * For email/password login we require the user to already have a profile
     * node in the database. If not — they may have been deleted or registered
     * with a different method. We sign them out and show an error.
     */
    private void verifyUserInDatabase(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        dbRef.child(Constants.NODE_USERS).child(uid).child(Constants.NODE_PROFILE)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || binding == null) return;
                        setLoading(false);

                        if (!snapshot.exists()) {
                            firebaseAuth.signOut();
                            showToast("No account found. Please register first.");
                            return;
                        }

                        // ✅ Cache full profile (name + phone + address) into SessionManager
                        String name = snapshot.child("name").getValue(String.class);
                        if (name == null || name.isEmpty()) name = firebaseUser.getEmail();

                        sessionManager.saveSession(uid, name, Constants.ROLE_USER);
                        cacheProfileToSession(snapshot);

                        navigateTo(UserDashboardActivity.class);
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
                        handleGoogleUserProfile(firebaseAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        showToast("Google authentication failed.");
                    }
                });
    }

    /**
     * Google login is more permissive — if no profile exists we create one
     * automatically (first time Google sign-in). If it does, just load it.
     */
    private void handleGoogleUserProfile(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        dbRef.child(Constants.NODE_USERS).child(uid).child(Constants.NODE_PROFILE)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || binding == null) return;

                        if (!snapshot.exists()) {
                            // First-time Google login → create profile node
                            String name  = firebaseUser.getDisplayName() != null
                                    ? firebaseUser.getDisplayName() : "User";
                            String email = firebaseUser.getEmail() != null
                                    ? firebaseUser.getEmail() : "";

                            User newUser = new User(uid, name, email, Constants.ROLE_USER);

                            dbRef.child(Constants.NODE_USERS).child(uid)
                                    .child(Constants.NODE_PROFILE)
                                    .setValue(newUser)
                                    .addOnSuccessListener(unused -> {
                                        if (!isAdded() || binding == null) return;
                                        setLoading(false);
                                        sessionManager.saveSession(uid, name, Constants.ROLE_USER);
                                        navigateTo(UserDashboardActivity.class);
                                    })
                                    .addOnFailureListener(e -> {
                                        if (!isAdded() || binding == null) return;
                                        setLoading(false);
                                        showToast("Failed to create profile: " + e.getMessage());
                                    });
                        } else {
                            // Existing user — load profile and cache
                            setLoading(false);
                            String name = snapshot.child("name").getValue(String.class);
                            if (name == null || name.isEmpty()) name = firebaseUser.getEmail();
                            sessionManager.saveSession(uid, name, Constants.ROLE_USER);
                            cacheProfileToSession(snapshot);
                            navigateTo(UserDashboardActivity.class);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isAdded() || binding == null) return;
                        setLoading(false);
                        showToast("Database error: " + error.getMessage());
                    }
                });
    }

    // ── Cache profile fields into SessionManager ──────────────────────────────

    /**
     * ✅ PHONE FIX — After any successful login, read phone + address from
     * the Firebase profile snapshot and persist them to SessionManager.
     * This ensures CheckoutActivity can read phone without a Firebase call.
     */
    private void cacheProfileToSession(DataSnapshot snapshot) {
        String phone   = snapshot.child("phone").getValue(String.class);
        String address = snapshot.child("address").getValue(String.class);
        String city    = snapshot.child("city").getValue(String.class);
        String pincode = snapshot.child("pincode").getValue(String.class);

        if (phone   != null && !phone.isEmpty())   sessionManager.saveUserPhone(phone);
        if (address != null && !address.isEmpty()
                && city    != null && pincode != null) {
            sessionManager.saveUserAddress(city, address, pincode);
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
        binding.btnGoogle.setEnabled(!loading);
    }

    private void showToast(String msg) {
        if (getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }
}