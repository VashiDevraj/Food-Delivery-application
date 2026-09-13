package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.adapters.AuthPagerAdapter;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * AuthActivity — entry screen with two tabs: User | Admin
 * Automatically redirects to correct dashboard if already logged in.
 */
public class AuthActivity extends AppCompatActivity {

    private TabLayout      tabLayout;
    private ViewPager2     viewPager;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        sessionManager = new SessionManager(this);

        // Auto-redirect if session exists
        if (sessionManager.isLoggedIn()) {
            redirectToDashboard();
            return;
        }

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new AuthPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(position == 0 ? "User" : "Admin")
        ).attach();
    }

    private void redirectToDashboard() {
        if (sessionManager.isAdmin()) {
            // Admin: check if restaurant is set up
            if (!sessionManager.isRestaurantSetupDone()) {
                startActivity(new Intent(this, RestaurantSetupActivity.class));
            } else {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            }
        } else {
            startActivity(new Intent(this, UserDashboardActivity.class));
        }
        finish();
    }
}