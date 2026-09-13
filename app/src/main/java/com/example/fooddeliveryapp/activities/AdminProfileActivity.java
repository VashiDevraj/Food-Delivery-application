package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.fooddeliveryapp.databinding.ActivityAdminProfileBinding;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminProfileActivity extends AppCompatActivity {

    private ActivityAdminProfileBinding binding;
    private SessionManager              sessionManager;
    private DatabaseReference           dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding        = ActivityAdminProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sessionManager = new SessionManager(this);
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadAdminProfile();

        setupClickListeners();
    }

    private void loadAdminProfile() {
        String name = sessionManager.getName();
        if (name != null && !name.isEmpty()) {
            binding.tvAdminName.setText(name);
            binding.tvAdminInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
        String uid = sessionManager.getUid();
        if (uid == null || uid.isEmpty()) return;
        dbRef.child(Constants.NODE_ADMINS).child(uid).child(Constants.NODE_PROFILE)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String fbName  = snapshot.child("name").getValue(String.class);
                        String fbEmail = snapshot.child("email").getValue(String.class);
                        if (fbName != null && !fbName.isEmpty()) {
                            binding.tvAdminName.setText(fbName);
                            binding.tvAdminInitial.setText(
                                    String.valueOf(fbName.charAt(0)).toUpperCase());
                            sessionManager.saveUserName(fbName);
                        }
                        if (fbEmail != null) binding.tvAdminEmail.setText(fbEmail);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Dark mode switch — wired to DarkModeManager ───────────────────────────


    private void setupClickListeners() {
        binding.btnEditProfile.setOnClickListener(v -> showEditNameDialog());

        binding.btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));
        binding.btnManageOrders.setOnClickListener(v ->
                startActivity(new Intent(this, AdminOrdersActivity.class)));
        binding.btnManageMenu.setOnClickListener(v ->
                startActivity(new Intent(this, AdminMenuActivity.class)));
        binding.btnRestaurantSettings.setOnClickListener(v ->
                startActivity(new Intent(this, RestaurantEditActivity.class)));
        binding.btnReviews.setOnClickListener(v ->
                startActivity(new Intent(this, AdminReviewsActivity.class)));
        binding.btnBestSellers.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBestSellersActivity.class)));
        binding.btnAppInfo.setOnClickListener(v -> showAboutDialog());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void showEditNameDialog() {
        TextInputEditText et = new TextInputEditText(this);
        et.setHint("Display Name");
        et.setText(sessionManager.getName());
        et.setPadding(48, 24, 48, 24);

        new MaterialAlertDialogBuilder(this)
                .setTitle("✏️ Edit Display Name")
                .setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String newName = et.getText() != null ? et.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sessionManager.saveUserName(newName);
                    binding.tvAdminName.setText(newName);
                    binding.tvAdminInitial.setText(String.valueOf(newName.charAt(0)).toUpperCase());
                    String uid = sessionManager.getUid();
                    if (uid != null && !uid.isEmpty()) {
                        dbRef.child(Constants.NODE_ADMINS).child(uid).child(Constants.NODE_PROFILE)
                                .child("name").setValue(newName)
                                .addOnSuccessListener(u ->
                                        Toast.makeText(this, "Name updated ✅", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("ℹ️ App Info")
                .setMessage("Food Delivery App — Admin Panel\nVersion 1.0.0\n\nManage your restaurant, menu, and orders.\n\nFor support, contact your developer.")
                .setPositiveButton("OK", null).show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    sessionManager.logout();
                    Intent intent = new Intent(this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null).show();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}