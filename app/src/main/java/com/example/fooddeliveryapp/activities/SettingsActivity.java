package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.fooddeliveryapp.databinding.ActivitySettingsBinding;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Restore dark mode state
        boolean isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
        binding.switchDarkMode.setChecked(isDark);

        // Restore notification prefs
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        binding.switchOrderNotifications.setChecked(prefs.getBoolean("notif_orders", true));
        binding.switchPromoNotifications.setChecked(prefs.getBoolean("notif_promo", true));

        setupListeners();
    }

    private void setupListeners() {
        // Dark mode toggle — immediately applies & persists
        binding.switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            int mode = checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(mode);
            getSharedPreferences("AppSettings", MODE_PRIVATE)
                    .edit().putInt("night_mode", mode).apply();
        });

        // Notification prefs (persist to SharedPreferences)
        binding.switchOrderNotifications.setOnCheckedChangeListener((btn, checked) ->
                getSharedPreferences("AppSettings", MODE_PRIVATE)
                        .edit().putBoolean("notif_orders", checked).apply());

        binding.switchPromoNotifications.setOnCheckedChangeListener((btn, checked) ->
                getSharedPreferences("AppSettings", MODE_PRIVATE)
                        .edit().putBoolean("notif_promo", checked).apply());

        // Text size picker
        binding.llTextSize.setOnClickListener(v -> {
            String[] sizes = {"Small", "Normal", "Large"};
            SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
            int current = prefs.getInt("text_size", 1);
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Text Size")
                    .setSingleChoiceItems(sizes, current, null)
                    .setPositiveButton("Apply", (d, w) -> {
                        int sel = ((androidx.appcompat.app.AlertDialog) d)
                                .getListView().getCheckedItemPosition();
                        getSharedPreferences("AppSettings", MODE_PRIVATE)
                                .edit().putInt("text_size", sel).apply();
                        binding.tvTextSize.setText(sizes[sel]);
                        Toast.makeText(this, "Restart the app to apply text size", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Privacy policy
        binding.llPrivacyPolicy.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://yourfoodapp.com/privacy")));
            } catch (Exception e) {
                Toast.makeText(this, "Browser not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Terms
        binding.llTerms.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://yourfoodapp.com/terms")));
            } catch (Exception e) {
                Toast.makeText(this, "Browser not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Clear cache
        binding.llClearCache.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Clear Cache")
                        .setMessage("This will clear cached images and temporary data. Your account data will not be affected.")
                        .setPositiveButton("Clear", (d, w) -> {
                            // Clear image caches etc.
                            sessionManager.clearNonAuthData();
                            Toast.makeText(this, "Cache cleared ✅", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}