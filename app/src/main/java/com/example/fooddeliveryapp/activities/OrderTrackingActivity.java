package com.example.fooddeliveryapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.Order;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * OrderTrackingActivity — User's live order tracking screen. Maps REMOVED.
 *
 * Shows:
 *  • Status header (current status label)
 *  • 5-step timeline: Placed → Preparing → Picked Up → Out for Delivery → Delivered
 *  • OTP card: ONLY shown for PREPAID orders when status is OUT_FOR_DELIVERY.
 *              Reads deliveryOtp from Firebase in real time.
 *              Hides OTP and shows "✅ Delivered!" message when status = DELIVERED.
 *  • Delivery boy card (name + phone), shown once a delivery boy is assigned.
 *
 * All data auto-refreshes via Firebase ValueEventListener.
 */
public class OrderTrackingActivity extends AppCompatActivity {

    private String orderId;
    private Order  currentOrder;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView         tvTrackOrderId, tvTrackStatus, tvDeliveryBoyName,
                             tvDeliveryBoyPhone, tvOtpCode, tvOtpHint, tvPickedUpStatus;
    private MaterialCardView cardOtp, cardDeliveryBoy;

    // Status step views
    private View stepPlaced, stepPreparing, stepPickedUp, stepOutDelivery, stepDelivered;
    private View linePlacedPreparing, linePreparingPickedUp, linePickedUpOut, lineOutDelivered;

    private DatabaseReference  dbRef;
    private SessionManager     sessionManager;
    private ValueEventListener orderListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        dbRef          = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        sessionManager = new SessionManager(this);
        orderId        = getIntent().getStringExtra("orderId");

        if (orderId == null || orderId.isEmpty()) { finish(); return; }

        Toolbar toolbar = findViewById(R.id.toolbarTracking);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Track Order");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        listenToOrder();
    }

    private void bindViews() {
        tvTrackOrderId     = findViewById(R.id.tvTrackOrderId);
        tvTrackStatus      = findViewById(R.id.tvTrackStatus);
        tvDeliveryBoyName  = findViewById(R.id.tvDeliveryBoyName);
        tvDeliveryBoyPhone = findViewById(R.id.tvDeliveryBoyPhone);
        tvOtpCode          = findViewById(R.id.tvOtpCode);
        tvOtpHint          = findViewById(R.id.tvOtpHint);
        tvPickedUpStatus   = findViewById(R.id.tvPickedUpStatus);
        cardOtp            = findViewById(R.id.cardOtp);
        cardDeliveryBoy    = findViewById(R.id.cardDeliveryBoy);

        stepPlaced            = findViewById(R.id.stepPlaced);
        stepPreparing         = findViewById(R.id.stepPreparing);
        stepPickedUp          = findViewById(R.id.stepPickedUp);
        stepOutDelivery       = findViewById(R.id.stepOutDelivery);
        stepDelivered         = findViewById(R.id.stepDelivered);
        linePlacedPreparing   = findViewById(R.id.linePlacedPreparing);
        linePreparingPickedUp = findViewById(R.id.linePreparingPickedUp);
        linePickedUpOut       = findViewById(R.id.linePickedUpOut);
        lineOutDelivered      = findViewById(R.id.lineOutDelivered);
    }

    // ── Real-time Firebase listener ───────────────────────────────────────────

    private void listenToOrder() {
        orderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) return;
                currentOrder = snap.getValue(Order.class);
                if (currentOrder == null) return;
                currentOrder.setOrderId(snap.getKey());

                String status = currentOrder.getStatus();

                // Update order ID + status label
                if (tvTrackOrderId != null)
                    tvTrackOrderId.setText("#" + orderId
                            .substring(0, Math.min(8, orderId.length())).toUpperCase());
                if (tvTrackStatus != null)
                    tvTrackStatus.setText(getStatusDisplay(status));

                // Picked-up hint
                if (tvPickedUpStatus != null) {
                    boolean pickedUp = Constants.STATUS_PICKED_UP.equals(status)
                            || Constants.STATUS_OUT_FOR_DELIVERY.equals(status)
                            || Constants.STATUS_DELIVERED.equals(status);
                    tvPickedUpStatus.setVisibility(pickedUp ? View.VISIBLE : View.GONE);
                    if (Constants.STATUS_PICKED_UP.equals(status))
                        tvPickedUpStatus.setText("🍱 Food has been picked up from restaurant!");
                    else if (Constants.STATUS_OUT_FOR_DELIVERY.equals(status))
                        tvPickedUpStatus.setText("🛵 Delivery partner is on the way!");
                }

                // Update timeline steps
                updateTimeline(status);

                // ── OTP card: only for PREPAID when Out for Delivery or Delivered ───
                String  otp       = snap.child("deliveryOtp").getValue(String.class);
                boolean isPrepaid = !currentOrder.isCOD();
                boolean isDelivered = Constants.STATUS_DELIVERED.equals(status);
                boolean isOutForDelivery = Constants.STATUS_OUT_FOR_DELIVERY.equals(status);

                if (isPrepaid && (isOutForDelivery || isDelivered)
                        && otp != null && !otp.isEmpty()) {
                    if (cardOtp != null) cardOtp.setVisibility(View.VISIBLE);

                    if (isDelivered) {
                        // After delivery, hide the actual OTP number
                        if (tvOtpCode != null) tvOtpCode.setText("✅");
                        if (tvOtpHint != null)
                            tvOtpHint.setText("OTP verified — Order successfully delivered!");
                    } else {
                        // Show OTP for user to share with delivery boy
                        if (tvOtpCode != null) tvOtpCode.setText(otp);
                        if (tvOtpHint != null)
                            tvOtpHint.setText("Share this OTP with your delivery partner to confirm delivery");
                    }
                } else {
                    if (cardOtp != null) cardOtp.setVisibility(View.GONE);
                }

                // ── Delivery boy info ─────────────────────────────────────────
                String dboyId   = currentOrder.getDeliveryBoyId();
                String dboyName = currentOrder.getDeliveryBoyName();
                if (dboyId != null && !dboyId.isEmpty()) {
                    if (cardDeliveryBoy != null) cardDeliveryBoy.setVisibility(View.VISIBLE);
                    if (tvDeliveryBoyName != null)
                        tvDeliveryBoyName.setText("🛵 " + (dboyName != null ? dboyName : "Delivery Partner"));
                    // Load phone from profile
                    dbRef.child(Constants.NODE_DELIVERY_BOYS).child(dboyId)
                            .child(Constants.NODE_PROFILE).child("phone")
                            .get().addOnSuccessListener(s -> {
                                String phone = s.getValue(String.class);
                                if (tvDeliveryBoyPhone != null && phone != null)
                                    tvDeliveryBoyPhone.setText("📞 " + phone);
                            });
                } else {
                    if (cardDeliveryBoy != null) cardDeliveryBoy.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        dbRef.child(Constants.NODE_ORDERS).child(orderId).addValueEventListener(orderListener);
    }

    // ── Timeline coloring ─────────────────────────────────────────────────────

    private void updateTimeline(String status) {
        int activeColor    = 0xFFE23744;
        int deliveredColor = 0xFF2E7D32;
        int inactiveColor  = 0xFFBDBDBD;

        boolean step2 = isAtOrAfter(status, Constants.STATUS_PREPARING);
        boolean step3 = isAtOrAfter(status, Constants.STATUS_PICKED_UP);
        boolean step4 = isAtOrAfter(status, Constants.STATUS_OUT_FOR_DELIVERY);
        boolean step5 = Constants.STATUS_DELIVERED.equals(status);

        setStepColor(stepPlaced,      activeColor);
        setStepColor(stepPreparing,   step2 ? activeColor : inactiveColor);
        setStepColor(stepPickedUp,    step3 ? activeColor : inactiveColor);
        setStepColor(stepOutDelivery, step4 ? activeColor : inactiveColor);
        setStepColor(stepDelivered,   step5 ? deliveredColor : inactiveColor);

        setLineColor(linePlacedPreparing,   step2 ? activeColor : inactiveColor);
        setLineColor(linePreparingPickedUp, step3 ? activeColor : inactiveColor);
        setLineColor(linePickedUpOut,       step4 ? activeColor : inactiveColor);
        setLineColor(lineOutDelivered,      step5 ? deliveredColor : inactiveColor);
    }

    private boolean isAtOrAfter(String current, String target) {
        String[] order = {
                Constants.STATUS_PENDING,
                Constants.STATUS_PREPARING,
                Constants.STATUS_PICKED_UP,
                Constants.STATUS_OUT_FOR_DELIVERY,
                Constants.STATUS_DELIVERED,
                Constants.STATUS_CANCELLED
        };
        int ci = indexOf(order, current);
        int ti = indexOf(order, target);
        return ci >= ti && ti >= 0;
    }

    private int indexOf(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] != null && arr[i].equals(val)) return i;
        return -1;
    }

    private void setStepColor(View v, int color) {
        if (v != null)
            v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void setLineColor(View v, int color) {
        if (v != null) v.setBackgroundColor(color);
    }

    private String getStatusDisplay(String status) {
        if (status == null) return "⏳ Order Placed";
        switch (status) {
            case Constants.STATUS_PENDING:          return "⏳ Order Placed";
            case Constants.STATUS_PREPARING:        return "🍳 Preparing Your Food";
            case Constants.STATUS_PICKED_UP:        return "🍱 Food Picked Up";
            case Constants.STATUS_OUT_FOR_DELIVERY: return "🛵 Out for Delivery";
            case Constants.STATUS_DELIVERED:        return "✅ Delivered!";
            case Constants.STATUS_CANCELLED:        return "❌ Cancelled";
            default:                                return "⏳ Processing";
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null)
            dbRef.child(Constants.NODE_ORDERS).child(orderId).removeEventListener(orderListener);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
