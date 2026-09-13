package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.databinding.ActivityHelpSupportBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class HelpSupportActivity extends AppCompatActivity {

    private ActivityHelpSupportBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHelpSupportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupFaqListeners();

        binding.btnLiveChat.setOnClickListener(v ->
                Toast.makeText(this, "Live chat coming soon!", Toast.LENGTH_SHORT).show());

        binding.btnEmailSupport.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:support@foodapp.com"));
            email.putExtra(Intent.EXTRA_SUBJECT, "Support Request - Food Delivery App");
            startActivity(Intent.createChooser(email, "Send Email"));
        });
    }

    private void setupFaqListeners() {
        binding.llFaqOrder.setOnClickListener(v ->
                showFaqDialog("Where is my order?",
                        "You can track your order in real time by going to:\n\nProfile → Your Orders → Select your active order\n\nYou will see live status updates from the restaurant and delivery partner. Typical delivery time is 30–45 minutes."));

        binding.llFaqPayment.setOnClickListener(v ->
                showFaqDialog("Payment issues",
                        "If your payment failed:\n\n• Check if your card/UPI is active\n• Ensure sufficient balance\n• Try a different payment method\n\nIf amount was deducted but order was not placed, it will be automatically refunded within 5–7 business days. Contact your bank for faster resolution."));

        binding.llFaqCancel.setOnClickListener(v ->
                showFaqDialog("How to cancel an order?",
                        "You can cancel an order within 2 minutes of placing it:\n\nProfile → Your Orders → Select order → Cancel\n\nOnce the restaurant has started preparing your order, cancellation may not be possible. Contact us at support@foodapp.com for help."));

        binding.llFaqRefund.setOnClickListener(v ->
                showFaqDialog("Refunds and returns",
                        "Refunds are processed within 5–7 business days back to your original payment method.\n\nEligible cases:\n• Order not delivered\n• Wrong items delivered\n• Food quality issues\n\nFor refund requests, email support@foodapp.com with your order ID."));
    }

    private void showFaqDialog(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); return true;
    }
}