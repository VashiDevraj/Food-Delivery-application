package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fooddeliveryapp.adapters.CouponAdapter;
import com.example.fooddeliveryapp.databinding.ActivityCouponBinding;
import com.example.fooddeliveryapp.models.Coupon;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * CouponActivity — displays available coupons and validates before returning.
 *
 * ── Coupon rules ─────────────────────────────────────────────────────────────
 * 1. Maximum 2 coupons can be applied at once (enforced in CartActivity too).
 * 2. "FREEDEL" (free delivery) requires the ₹40 delivery fee to exist first;
 *    it zeroes it out and returns isFreeDelivery=true.
 * 3. minOrderValue checked against real cartSubtotal passed from CartActivity.
 * 4. isNewUserOnly checked via Firebase order count.
 * 5. Already-applied coupon codes are disabled in the list.
 *
 * ── Intent extras IN ─────────────────────────────────────────────────────────
 *   cartSubtotal  (double) — item subtotal from CartActivity          [BUG FIX]
 *   fillingSlot   (int)    — 1 or 2; which slot is being filled
 *   appliedCode1  (String) — already applied code in slot 1 (may be "")
 *   appliedCode2  (String) — already applied code in slot 2 (may be "")
 *
 * ── Intent extras OUT ────────────────────────────────────────────────────────
 *   couponCode    (String)  — code of applied coupon
 *   discount      (double)  — monetary discount amount (0 for FREEDEL)
 *   isFreeDelivery(boolean) — true only for FREEDEL coupon
 *   slot          (int)     — slot this coupon fills (1 or 2)
 */
public class CouponActivity extends AppCompatActivity {

    private ActivityCouponBinding binding;
    private SessionManager        sessionManager;
    private DatabaseReference     dbRef;

    private List<Coupon> allCoupons = new ArrayList<>();

    /** Real cart subtotal — passed from CartActivity (BUG FIX: was always 0) */
    private double cartSubtotal = 0;
    /** Slot we're currently filling: 1 or 2 */
    private int    fillingSlot  = 1;
    /** Already applied codes (disable them in the list) */
    private String appliedCode1 = "";
    private String appliedCode2 = "";

    // Delivery fee constant — must match CartActivity
    private static final double DELIVERY_FEE = 40.0;
    private static final String COUPON_FREE_DELIVERY = "FREEDEL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCouponBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        // ── Read intent extras ────────────────────────────────────────────
        cartSubtotal = getIntent().getDoubleExtra("cartSubtotal", 0);
        fillingSlot  = getIntent().getIntExtra("fillingSlot", 1);
        appliedCode1 = getIntent().getStringExtra("appliedCode1");
        appliedCode2 = getIntent().getStringExtra("appliedCode2");
        if (appliedCode1 == null) appliedCode1 = "";
        if (appliedCode2 == null) appliedCode2 = "";

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Apply Coupon");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        buildCouponList();

        // ── Populate header hints ─────────────────────────────────────────
        try {
            binding.tvCartSubtotalHint.setText(
                    "Cart total: ₹" + String.format("%.0f", cartSubtotal));
            binding.tvSlot1Label.setText(
                    appliedCode1.isEmpty() ? "Slot 1: Empty" : "Slot 1: " + appliedCode1);
            binding.tvSlot2Label.setText(
                    appliedCode2.isEmpty() ? "Slot 2: Empty" : "Slot 2: " + appliedCode2);
        } catch (Exception ignored) { /* view may not exist in older layout versions */ }

        binding.rvCoupons.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCoupons.setAdapter(
                new CouponAdapter(allCoupons, appliedCode1, appliedCode2,
                        this::handleCouponClick));
    }

    /** Define all coupons. */
    private void buildCouponList() {
        allCoupons.clear();
        //                    code          description                               disc  minOrder  newUserOnly
        allCoupons.add(new Coupon("WELCOME50",  "₹50 OFF on your first order",            50,   0,        true));
        allCoupons.add(new Coupon("NEWUSER30",  "₹30 OFF for new users",                  30,   0,        true));
        allCoupons.add(new Coupon("SAVE100",    "₹100 OFF on orders above ₹400",         100, 400,        false));
        allCoupons.add(new Coupon("SAVE50",     "₹50 OFF on orders above ₹250",           50, 250,        false));
        allCoupons.add(new Coupon("FREEDEL",    "Free delivery on any order",               0,   0,        false));
        allCoupons.add(new Coupon("BIGORDER",   "₹150 OFF on orders above ₹700",         150, 700,        false));
    }

    /**
     * Validates the coupon before applying:
     *   1. Already applied? → reject
     *   2. Free delivery coupon (FREEDEL) — special handling
     *   3. Min-order check
     *   4. New-user check via Firebase
     */
    private void handleCouponClick(Coupon coupon) {
        String code = coupon.getCode();

        // ── Already applied? ──────────────────────────────────────────────
        if (code.equals(appliedCode1) || code.equals(appliedCode2)) {
            Toast.makeText(this, "\"" + code + "\" is already applied.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── FREE DELIVERY special rule ────────────────────────────────────
        // FREEDEL can only be in slot 2 (it replaces the delivery fee).
        // The delivery fee (₹40) is always present, so there is nothing to
        // "add first" — we just zero it out. Show a clear confirmation.
        if (COUPON_FREE_DELIVERY.equals(code)) {
            if (fillingSlot == 1 && !appliedCode1.isEmpty()) {
                // Slot 1 taken by a discount; FREEDEL should go to slot 2
                Toast.makeText(this,
                        "FREEDEL will be applied as your second coupon\n" +
                                "and removes the ₹40 delivery fee.", Toast.LENGTH_LONG).show();
            }
            // Return free delivery result
            Intent data = new Intent();
            data.putExtra("couponCode",     code);
            data.putExtra("discount",       0.0);   // monetary = 0; delivery handled separately
            data.putExtra("isFreeDelivery", true);
            data.putExtra("slot",           2);      // FREEDEL always slot 2
            setResult(RESULT_OK, data);
            Toast.makeText(this, "Free delivery applied! 🚚", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── Min-order check ───────────────────────────────────────────────
        if (coupon.getMinOrderValue() > 0 && cartSubtotal < coupon.getMinOrderValue()) {
            Toast.makeText(this,
                    "\"" + code + "\" requires a minimum order of ₹"
                            + String.format("%.0f", coupon.getMinOrderValue())
                            + ".\nYour cart: ₹" + String.format("%.0f", cartSubtotal),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // ── New-user check ────────────────────────────────────────────────
        if (coupon.isNewUserOnly()) {
            checkIsNewUserThenApply(coupon);
        } else {
            applyCoupon(coupon);
        }
    }

    private void checkIsNewUserThenApply(Coupon coupon) {
        String userId = sessionManager.getUid();
        if (userId == null || userId.isEmpty()) {
            applyCoupon(coupon);
            return;
        }

        binding.progressCoupon.setVisibility(View.VISIBLE);

        dbRef.child(Constants.NODE_USERS).child(userId).child(Constants.NODE_MY_ORDERS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        binding.progressCoupon.setVisibility(View.GONE);
                        long orderCount = snapshot.getChildrenCount();
                        if (orderCount > 0) {
                            Toast.makeText(CouponActivity.this,
                                    "\"" + coupon.getCode() + "\" is only valid for first-time orders.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            applyCoupon(coupon);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressCoupon.setVisibility(View.GONE);
                        applyCoupon(coupon); // optimistic allow
                    }
                });
    }

    private void applyCoupon(Coupon coupon) {
        Intent data = new Intent();
        data.putExtra("couponCode",     coupon.getCode());
        data.putExtra("discount",       (double) coupon.getDiscount());
        data.putExtra("isFreeDelivery", false);
        data.putExtra("slot",           fillingSlot);
        setResult(RESULT_OK, data);
        Toast.makeText(this,
                "Coupon \"" + coupon.getCode() + "\" applied! 🎉 -₹" + coupon.getDiscount(),
                Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}