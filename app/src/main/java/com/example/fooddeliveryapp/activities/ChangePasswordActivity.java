package com.example.fooddeliveryapp.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.databinding.ActivityChangePasswordBinding;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private FirebaseAuth firebaseAuth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth   = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupPasswordStrengthWatcher();
        setupClickListeners();
    }

    private void setupPasswordStrengthWatcher() {
        binding.etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String pass = s.toString();
                if (pass.isEmpty()) {
                    binding.tvPasswordStrength.setVisibility(View.GONE);
                    return;
                }
                binding.tvPasswordStrength.setVisibility(View.VISIBLE);
                int strength = getPasswordStrength(pass);
                if (strength == 0) {
                    binding.tvPasswordStrength.setText("Strength: Weak ❌");
                    binding.tvPasswordStrength.setTextColor(0xFFE53935);
                } else if (strength == 1) {
                    binding.tvPasswordStrength.setText("Strength: Fair ⚠️");
                    binding.tvPasswordStrength.setTextColor(0xFFFFA000);
                } else {
                    binding.tvPasswordStrength.setText("Strength: Strong ✅");
                    binding.tvPasswordStrength.setTextColor(0xFF388E3C);
                }
            }
        });
    }

    private int getPasswordStrength(String password) {
        boolean hasUpper   = password.matches(".*[A-Z].*");
        boolean hasDigit   = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");
        boolean hasLength  = password.length() >= 8;
        int score = (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0)
                + (hasSpecial ? 1 : 0) + (hasLength ? 1 : 0);
        if (score <= 1) return 0; // weak
        if (score <= 2) return 1; // fair
        return 2;                 // strong
    }

    private void setupClickListeners() {
        binding.btnChangePassword.setOnClickListener(v -> attemptChangePassword());
        binding.tvForgotPassword.setOnClickListener(v -> sendPasswordResetEmail());
    }

    private void attemptChangePassword() {
        String currentPass  = getTextFrom(binding.etCurrentPassword);
        String newPass      = getTextFrom(binding.etNewPassword);
        String confirmPass  = getTextFrom(binding.etConfirmPassword);

        // Validate
        if (TextUtils.isEmpty(currentPass)) {
            binding.tilCurrentPassword.setError("Enter your current password"); return;
        }
        binding.tilCurrentPassword.setError(null);

        if (TextUtils.isEmpty(newPass)) {
            binding.tilNewPassword.setError("Enter a new password"); return;
        }
        if (newPass.length() < 8) {
            binding.tilNewPassword.setError("Password must be at least 8 characters"); return;
        }
        if (!newPass.matches(".*[0-9].*")) {
            binding.tilNewPassword.setError("Password must contain at least one number"); return;
        }
        if (!newPass.matches(".*[A-Z].*")) {
            binding.tilNewPassword.setError("Password must contain at least one uppercase letter"); return;
        }
        binding.tilNewPassword.setError(null);

        if (!newPass.equals(confirmPass)) {
            binding.tilConfirmPassword.setError("Passwords do not match"); return;
        }
        binding.tilConfirmPassword.setError(null);

        if (newPass.equals(currentPass)) {
            binding.tilNewPassword.setError("New password must be different from current"); return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        binding.btnChangePassword.setEnabled(false);
        binding.btnChangePassword.setText("Changing...");

        // Re-authenticate then change password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    user.updatePassword(newPass)
                            .addOnSuccessListener(v2 -> {
                                binding.btnChangePassword.setEnabled(true);
                                binding.btnChangePassword.setText("Change Password");
                                showSuccessDialog();
                            })
                            .addOnFailureListener(e -> {
                                binding.btnChangePassword.setEnabled(true);
                                binding.btnChangePassword.setText("Change Password");
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.btnChangePassword.setEnabled(true);
                    binding.btnChangePassword.setText("Change Password");
                    binding.tilCurrentPassword.setError("Current password is incorrect");
                });
    }

    private void sendPasswordResetEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "No email found for your account.", Toast.LENGTH_SHORT).show();
            return;
        }
        firebaseAuth.sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(unused ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Reset Email Sent")
                                .setMessage("A password reset link has been sent to:\n\n" + user.getEmail()
                                        + "\n\nCheck your inbox and follow the link to reset your password.")
                                .setPositiveButton("OK", null)
                                .show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send reset email: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("✅ Password Changed")
                .setMessage("Your password has been changed successfully. Please use your new password next time you log in.")
                .setPositiveButton("Done", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private String getTextFrom(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}