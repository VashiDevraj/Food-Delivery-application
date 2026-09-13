package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.databinding.ActivitySplashBinding;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sessionManager = new SessionManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (sessionManager.isLoggedIn()) {
                if (sessionManager.isAdmin()) {
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                } else if (sessionManager.isDeliveryBoy()) {
                    // Route delivery boy to their dedicated dashboard
                    startActivity(new Intent(this, DeliveryBoyDashboardActivity.class));
                } else {
                    startActivity(new Intent(this, UserDashboardActivity.class));
                }
            } else {
                startActivity(new Intent(this, AuthActivity.class));
            }
            finish();
        }, Constants.SPLASH_DELAY);
    }
}