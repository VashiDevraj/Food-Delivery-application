package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.example.fooddeliveryapp.models.Address;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

/**
 * CheckoutActivity
 * - Receives coupon/discount already applied from CartActivity
 * - Shows order summary + delivery address
 * - Tip chips: ₹10 / ₹20 / ₹50 / Custom
 * - Delivery note field for delivery boy
 * - NO coupon input here — coupon is handled in CartActivity
 */
public class CheckoutActivity extends AppCompatActivity {

    private TextView          tvCheckoutSubtotal, tvCheckoutAddress, tvCheckoutDiscount,
            tvTipSelected, tvDeliveryFee, tvGrandTotal,
            tvCouponApplied, tvCouponSavings;
    private ChipGroup         chipGroupTip;
    private Chip              chipTip10, chipTip20, chipTip50, chipTipCustom;
    private TextInputEditText etCustomTip, etDeliveryNote;
    private MaterialButton    btnProceedPayment;
    private View              layoutDiscountRow;
    TextView                  btnChangeAddress;
    private SessionManager    sessionManager;

    private double  subtotal       = 0;
    private double  tipAmount      = 0;
    private double  discount       = 0;
    private double  deliveryFee    = 40.0;
    private String  restaurantId   = "";
    private String  restaurantName = "";
    private String  couponCode     = "";
    private boolean customTipMode  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        sessionManager = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbarCheckout);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Checkout");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Read all values forwarded from CartActivity
        subtotal       = getIntent().getDoubleExtra("totalAmount", 0);
        discount       = getIntent().getDoubleExtra("discount",    0);
        deliveryFee    = getIntent().getDoubleExtra("deliveryFee", 40.0);
        restaurantId   = safe(getIntent().getStringExtra("restaurantId"));
        restaurantName = safe(getIntent().getStringExtra("restaurantName"));
        couponCode     = safe(getIntent().getStringExtra("couponCode"));

        bindViews();
        populateAddress();
        showCouponBadge();
        updateTotals();
        setupTipChips();
        btnChangeAddress.setOnClickListener(v -> {
            startActivity(new Intent(this, AddressListActivity.class));
        });

        btnProceedPayment.setOnClickListener(v -> proceedToPayment());
    }

    private void bindViews() {
        tvCheckoutSubtotal  = findViewById(R.id.tvCheckoutSubtotal);
        tvCheckoutAddress   = findViewById(R.id.tvCheckoutAddress);
        tvCheckoutDiscount  = findViewById(R.id.tvCheckoutDiscount);
        layoutDiscountRow   = findViewById(R.id.layoutDiscountRow);
        tvCouponApplied     = findViewById(R.id.tvCouponApplied);
        tvCouponSavings     = findViewById(R.id.tvCouponSavings);
        tvTipSelected       = findViewById(R.id.tvTipSelected);
        tvDeliveryFee       = findViewById(R.id.tvDeliveryFee);
        tvGrandTotal        = findViewById(R.id.tvGrandTotal);
        chipGroupTip        = findViewById(R.id.chipGroupTip);
        chipTip10           = findViewById(R.id.chipTip10);
        chipTip20           = findViewById(R.id.chipTip20);
        chipTip50           = findViewById(R.id.chipTip50);
        chipTipCustom       = findViewById(R.id.chipTipCustom);
        etCustomTip         = findViewById(R.id.etCustomTip);
        etDeliveryNote      = findViewById(R.id.etDeliveryNote);
        btnProceedPayment   = findViewById(R.id.btnProceedPayment);
        btnChangeAddress = findViewById(R.id.btnChangeAddress);

        tvCheckoutSubtotal.setText(String.format(Locale.getDefault(), "₹%.2f", subtotal));
        tvDeliveryFee.setText(String.format(Locale.getDefault(), "₹%.0f", deliveryFee));
    }

    private void populateAddress() {

        // 🔥 FIRST TRY SESSION (FAST + RELIABLE)
        SessionManager.Address address = sessionManager.getSelectedAddress();

        if (address != null && address.fullAddress != null && !address.fullAddress.isEmpty()) {
            tvCheckoutAddress.setText(address.fullAddress);
            return;
        }

        // 🔥 FALLBACK TO FIREBASE
        String uid = sessionManager.getUid();

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        dbRef.child("Users").child(uid).child("selectedAddressId")
                .get().addOnSuccessListener(snapshot -> {

                    String selectedId = snapshot.getValue(String.class);

                    if (selectedId == null) {
                        tvCheckoutAddress.setText("No address selected");
                        return;
                    }

                    dbRef.child("Users").child(uid)
                            .child("Addresses").child(selectedId)
                            .get().addOnSuccessListener(snap -> {

                                Address a = snap.getValue(Address.class);

                                if (a != null) {
                                    tvCheckoutAddress.setText(a.fullAddress);

                                    // 🔥 SYNC BACK TO SESSION
                                    sessionManager.saveUserAddress(
                                            a.city,
                                            a.fullAddress,
                                            a.pincode
                                    );
                                }
                            });

                });
    }

    /** Show coupon badge if a coupon was applied in CartActivity */
    private void showCouponBadge() {
        if (!couponCode.isEmpty() && discount > 0) {
            if (tvCouponApplied != null) {
                tvCouponApplied.setVisibility(View.VISIBLE);
                tvCouponApplied.setText("🎟️ " + couponCode + " applied");
            }
            if (tvCouponSavings != null) {
                tvCouponSavings.setVisibility(View.VISIBLE);
                tvCouponSavings.setText("You save ₹" + String.format("%.0f", discount) + " 🎉");
            }
            if (layoutDiscountRow != null) layoutDiscountRow.setVisibility(View.VISIBLE);
            if (tvCheckoutDiscount != null)
                tvCheckoutDiscount.setText("-₹" + String.format("%.0f", discount));
        } else {
            if (tvCouponApplied   != null) tvCouponApplied.setVisibility(View.GONE);
            if (tvCouponSavings   != null) tvCouponSavings.setVisibility(View.GONE);
            if (layoutDiscountRow != null) layoutDiscountRow.setVisibility(View.GONE);
        }
    }

    private void updateTotals() {
        double grand = subtotal - discount + deliveryFee + tipAmount;
        if (grand < 0) grand = 0;
        tvGrandTotal.setText(String.format(Locale.getDefault(), "₹%.2f", grand));
        tvTipSelected.setText(tipAmount > 0
                ? String.format("₹%.0f added 🎁", tipAmount) : "No tip selected");
        btnProceedPayment.setText(String.format(Locale.getDefault(),
                "Proceed to Pay  ₹%.2f", grand));
    }

    private void setupTipChips() {
        chipGroupTip.setOnCheckedStateChangeListener((group, checkedIds) -> {
            customTipMode = false;
            etCustomTip.setVisibility(View.GONE);

            if (checkedIds.isEmpty()) {
                tipAmount = 0;
            } else {
                int id = checkedIds.get(0);
                if      (id == R.id.chipTip10)    { tipAmount = 10; }
                else if (id == R.id.chipTip20)    { tipAmount = 20; }
                else if (id == R.id.chipTip50)    { tipAmount = 50; }
                else if (id == R.id.chipTipCustom) {
                    customTipMode = true;
                    etCustomTip.setVisibility(View.VISIBLE);
                    String raw = etCustomTip.getText() != null
                            ? etCustomTip.getText().toString().trim() : "";
                    try { tipAmount = raw.isEmpty() ? 0 : Double.parseDouble(raw); }
                    catch (NumberFormatException e) { tipAmount = 0; }
                }
            }
            updateTotals();
        });

        etCustomTip.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (customTipMode) {
                    try { tipAmount = s.length() > 0 ? Double.parseDouble(s.toString()) : 0; }
                    catch (NumberFormatException e) { tipAmount = 0; }
                    updateTotals();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void proceedToPayment() {
        SessionManager.Address address = sessionManager.getSelectedAddress();
        if (address == null || address.fullAddress.isEmpty()) {
            Toast.makeText(this, "Please add a delivery address first", Toast.LENGTH_SHORT).show();
            return;
        }

        String deliveryNote = etDeliveryNote.getText() != null
                ? etDeliveryNote.getText().toString().trim() : "";

        double grandTotal = subtotal - discount + deliveryFee + tipAmount;
        if (grandTotal < 0) grandTotal = 0;

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("address",        address.fullAddress);
        intent.putExtra("phone",          sessionManager.getUserPhone());
        intent.putExtra("restaurantId",   restaurantId);
        intent.putExtra("restaurantName", restaurantName);
        intent.putExtra("totalAmount",    grandTotal);
        intent.putExtra("subtotal",       subtotal);
        intent.putExtra("tipAmount",      tipAmount);
        intent.putExtra("deliveryNote",   deliveryNote);
        intent.putExtra("deliveryFee",    deliveryFee);
        intent.putExtra("discount",       discount);
        intent.putExtra("couponCode",     couponCode);
        startActivity(intent);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private static String safe(String s) { return s != null ? s : ""; }
}