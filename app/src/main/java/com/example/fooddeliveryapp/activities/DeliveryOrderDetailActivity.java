package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.Order;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * DeliveryOrderDetailActivity — Maps removed. Full sequential OTP + COD flow.
 *
 * DELIVERY FLOW (strictly enforced):
 *  1. ACCEPTED/PREPARING → "🍱 Pick Up Food" button
 *  2. PICKED_UP           → "🛵 Mark Out for Delivery" button
 *                           (OTP generated here for PREPAID orders and saved to Firebase)
 *  3. OUT_FOR_DELIVERY:
 *       • PREPAID  → "🔑 Generate OTP" shown first (writes OTP to Firebase),
 *                    then "🔐 Enter OTP" button (must match to mark delivered)
 *       • COD/POD  → COD section visible (Collect Cash OR UPI QR);
 *                    once codCollected==true → "✅ Mark as Delivered" shown
 *  4. DELIVERED → recordEarnings() → finish()
 *
 * UPI VPA: vashidevraj50@oksbi (all QR payments go here)
 * OTP is stored at Orders/{id}/deliveryOtp in Firebase Realtime Database.
 * User sees it in OrderTrackingActivity.
 */
public class DeliveryOrderDetailActivity extends AppCompatActivity {

    private String orderId;
    private Order  currentOrder;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView       tvOrderIdDetail, tvCustomerName, tvCustomerPhone, tvCustomerAddress,
            tvOrderItems, tvOrderAmount, tvPaymentMethod, tvPaymentStatus,
            tvTipAmount, tvDeliveryNote, tvOrderStatus, tvApproxEarning,
            tvCodAmount, tvStatusHint;
    private LinearLayout   layoutCodSection, layoutTipSection, layoutNoteSection;
    private MaterialButton btnPickupFood, btnMarkOutForDelivery, btnMarkDelivered,
            btnCollectCOD, btnShowUpiQr, btnEnterOtp, btnSendOtp;

    private DatabaseReference  dbRef;
    private SessionManager     sessionManager;
    private String             deliveryBoyUid;
    private ValueEventListener orderListener;

    // OTP state — refreshed from Firebase on every loadOrderDetails() call
    private boolean otpSentToUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_order_detail);

        dbRef          = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        sessionManager = new SessionManager(this);
        deliveryBoyUid = sessionManager.getUid();
        orderId        = getIntent().getStringExtra("orderId");
        if (orderId == null || orderId.isEmpty()) { finish(); return; }

        Toolbar toolbar = findViewById(R.id.toolbarDeliveryDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Order Details");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        loadOrderDetails();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews() {
        tvOrderIdDetail   = findViewById(R.id.tvOrderIdDetail);
        tvCustomerName    = findViewById(R.id.tvCustomerName);
        tvCustomerPhone   = findViewById(R.id.tvCustomerPhone);
        tvCustomerAddress = findViewById(R.id.tvCustomerAddress);
        tvOrderItems      = findViewById(R.id.tvOrderItemsDetail);
        tvOrderAmount     = findViewById(R.id.tvOrderAmountDetail);
        tvPaymentMethod   = findViewById(R.id.tvPaymentMethodDetail);
        tvPaymentStatus   = findViewById(R.id.tvPaymentStatusDetail);
        tvTipAmount       = findViewById(R.id.tvTipAmountDetail);
        tvDeliveryNote    = findViewById(R.id.tvDeliveryNoteDetail);
        tvOrderStatus     = findViewById(R.id.tvOrderStatusDetail);
        tvApproxEarning   = findViewById(R.id.tvApproxEarning);
        tvCodAmount       = findViewById(R.id.tvCodAmountDetail);
        tvStatusHint      = findViewById(R.id.tvStatusHint);

        layoutCodSection  = findViewById(R.id.layoutCodSection);
        layoutTipSection  = findViewById(R.id.layoutTipSection);
        layoutNoteSection = findViewById(R.id.layoutNoteSection);

        btnPickupFood         = findViewById(R.id.btnPickupFood);
        btnMarkOutForDelivery = findViewById(R.id.btnMarkOutForDelivery);
        btnMarkDelivered      = findViewById(R.id.btnMarkDelivered);
        btnCollectCOD         = findViewById(R.id.btnCollectCOD);
        btnShowUpiQr          = findViewById(R.id.btnShowUpiQr);
        btnEnterOtp           = findViewById(R.id.btnEnterOtp);
        btnSendOtp            = findViewById(R.id.btnSendOtp);
    }

    // ── Firebase load ─────────────────────────────────────────────────────────

    private void loadOrderDetails() {
        orderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) { finish(); return; }
                currentOrder = snap.getValue(Order.class);
                if (currentOrder == null) { finish(); return; }
                currentOrder.setOrderId(snap.getKey());

                // Check if OTP was already generated and saved to Firebase
                String existingOtp = snap.child("deliveryOtp").getValue(String.class);
                otpSentToUser = (existingOtp != null && !existingOtp.isEmpty());

                populateUI();
                setupActionButtons();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(DeliveryOrderDetailActivity.this,
                        "Failed to load order", Toast.LENGTH_SHORT).show();
            }
        };
        dbRef.child(Constants.NODE_ORDERS).child(orderId).addValueEventListener(orderListener);
    }

    // ── Populate UI ───────────────────────────────────────────────────────────

    private void populateUI() {
        Order o = currentOrder;
        if (tvOrderIdDetail   != null) tvOrderIdDetail.setText(o.getShortOrderId());
        if (tvCustomerName    != null) tvCustomerName.setText(o.getUserName());
        if (tvCustomerPhone   != null) tvCustomerPhone.setText(o.getPhone());
        if (tvCustomerAddress != null) tvCustomerAddress.setText(o.getAddress());
        if (tvOrderAmount     != null) tvOrderAmount.setText(String.format("₹%.2f", o.getTotalAmount()));
        if (tvPaymentMethod   != null) tvPaymentMethod.setText(o.getPaymentMethod());
        if (tvPaymentStatus   != null) tvPaymentStatus.setText(o.getPaymentStatus());
        if (tvOrderStatus     != null) tvOrderStatus.setText(getStatusLabel(o.getStatus()));

        // COD amount label
        if (tvCodAmount != null)
            tvCodAmount.setText(String.format(
                    "Collect ₹%.2f from the customer before marking delivered.", o.getTotalAmount()));

        // Items list
        if (o.getItems() != null && !o.getItems().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            o.getItems().forEach((k, item) -> {
                if (item != null)
                    sb.append("• ").append(item.getName())
                            .append(" ×").append(item.getQuantity())
                            .append("  ₹").append(String.format("%.0f", item.getPrice() * item.getQuantity()))
                            .append("\n");
            });
            if (tvOrderItems != null) tvOrderItems.setText(sb.toString().trim());
        }

        // Tip section
        if (layoutTipSection != null) {
            if (o.getTipAmount() > 0) {
                layoutTipSection.setVisibility(View.VISIBLE);
                if (tvTipAmount != null)
                    tvTipAmount.setText("₹" + (int) o.getTipAmount() + " 🎁");
            } else {
                layoutTipSection.setVisibility(View.GONE);
            }
        }

        // Delivery note
        String note = o.getDeliveryNote();
        if (layoutNoteSection != null) {
            if (note != null && !note.isEmpty()) {
                layoutNoteSection.setVisibility(View.VISIBLE);
                if (tvDeliveryNote != null) tvDeliveryNote.setText(note);
            } else {
                layoutNoteSection.setVisibility(View.GONE);
            }
        }

        // Approx earning
        double commission  = Constants.BASE_DELIVERY_FEE +
                (o.getTotalAmount() * Constants.DELIVERY_COMMISSION_PERCENT / 100.0);
        double totalApprox = commission + o.getTipAmount();
        if (tvApproxEarning != null)
            tvApproxEarning.setText(String.format("~₹%.0f (₹%.0f base + ₹%.0f tip)",
                    totalApprox, commission, o.getTipAmount()));
    }

    private String getStatusLabel(String status) {
        if (status == null) return "Pending";
        switch (status) {
            case Constants.STATUS_PREPARING:        return "🍳 Preparing";
            case Constants.STATUS_PICKED_UP:        return "🍱 Picked Up";
            case Constants.STATUS_OUT_FOR_DELIVERY: return "🛵 Out for Delivery";
            case Constants.STATUS_DELIVERED:        return "✅ Delivered";
            case Constants.STATUS_CANCELLED:        return "❌ Cancelled";
            default:                                return status;
        }
    }

    // ── ACTION BUTTONS — strict sequential flow ───────────────────────────────
    /**
     * STEP 1 (PREPARING)      → btnPickupFood
     * STEP 2 (PICKED_UP)      → btnMarkOutForDelivery
     * STEP 3 (OUT_FOR_DELIVERY):
     *   COD   → layoutCodSection visible (Collect Cash + UPI QR)
     *           once codCollected == true → btnMarkDelivered
     *   PREPAID → if !otpSentToUser → btnSendOtp (generates OTP in Firebase)
     *             if otpSentToUser  → btnEnterOtp (opens OtpVerifyActivity)
     *             (btnMarkDelivered is revealed only after OTP verified)
     */
    private void setupActionButtons() {
        if (currentOrder == null) return;

        String  status        = currentOrder.getStatus();
        String  paymentMethod = currentOrder.getPaymentMethod();
        boolean isCOD         = "COD".equalsIgnoreCase(paymentMethod);

        // ── Hide everything first ──
        setVisible(btnPickupFood,         false);
        setVisible(btnMarkOutForDelivery, false);
        setVisible(btnMarkDelivered,      false);
        setVisible(btnSendOtp,            false);
        setVisible(btnEnterOtp,           false);
        if (layoutCodSection != null) layoutCodSection.setVisibility(View.GONE);
        if (tvStatusHint     != null) tvStatusHint.setVisibility(View.GONE);

        // ═══════════════════════════════════════════════════════════════════
        // STEP 1 — Pick Up Food
        // ═══════════════════════════════════════════════════════════════════
        if (Constants.STATUS_PREPARING.equals(status)) {

            setVisible(btnPickupFood, true);
            showHint("📍 Go to the restaurant and pick up the order");

            btnPickupFood.setOnClickListener(v -> {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", Constants.STATUS_PICKED_UP);
                writeOrderUpdates(updates);
                Toast.makeText(this, "Food Picked Up 🍱", Toast.LENGTH_SHORT).show();
            });
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 2 — Mark Out for Delivery
        // ═══════════════════════════════════════════════════════════════════
        else if (Constants.STATUS_PICKED_UP.equals(status)) {

            setVisible(btnMarkOutForDelivery, true);
            showHint("🛵 Head to the customer's address");

            btnMarkOutForDelivery.setOnClickListener(v -> {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", Constants.STATUS_OUT_FOR_DELIVERY);

                // Generate OTP now for PREPAID orders
                if (!isCOD) {
                    String otp = String.valueOf(1000 + new Random().nextInt(9000));
                    updates.put("deliveryOtp", otp);
                    updates.put("otpVerified", false);
                }

                writeOrderUpdates(updates);
                Toast.makeText(this, "Out for Delivery 🛵", Toast.LENGTH_SHORT).show();
            });
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 3 — Out for Delivery
        // ═══════════════════════════════════════════════════════════════════
        else if (Constants.STATUS_OUT_FOR_DELIVERY.equals(status)) {

            if (isCOD) {
                // ── COD flow ──────────────────────────────────────────────
                if (layoutCodSection != null) layoutCodSection.setVisibility(View.VISIBLE);
                setVisible(btnCollectCOD, true);
                setVisible(btnShowUpiQr,  true);

                boolean alreadyCollected = currentOrder.isCodCollected();

                if (alreadyCollected) {
                    showHint("✅ Payment collected — tap below to complete delivery");
                    setVisible(btnMarkDelivered, true);
                    // Disable collect buttons to prevent double-tap
                    if (btnCollectCOD != null) btnCollectCOD.setEnabled(false);
                    if (btnShowUpiQr  != null) btnShowUpiQr.setEnabled(false);
                } else {
                    showHint("💵 Collect payment from customer first");
                }

                // 💵 Collect Cash
                if (btnCollectCOD != null)
                    btnCollectCOD.setOnClickListener(v -> {
                        new AlertDialog.Builder(this)
                                .setTitle("💵 Confirm Cash Collection")
                                .setMessage(String.format(
                                        "Did you collect ₹%.2f cash from the customer?",
                                        currentOrder.getTotalAmount()))
                                .setPositiveButton("Yes, Collected ✅", (d, w) -> {
                                    Map<String, Object> u = new HashMap<>();
                                    u.put("codCollected", true);
                                    u.put("paymentStatus", Constants.PAYMENT_PAID);
                                    writeOrderUpdates(u);
                                    Toast.makeText(this,
                                            "Cash collection recorded ✅", Toast.LENGTH_SHORT).show();
                                    // UI will refresh via Firebase listener
                                })
                                .setNegativeButton("Not yet", null)
                                .show();
                    });

                // 📱 UPI QR (redirects to UPI app with vashidevraj50@oksbi)
                if (btnShowUpiQr != null)
                    btnShowUpiQr.setOnClickListener(v -> showUpiQrBottomSheet());

                // ✅ Mark Delivered (only after payment)
                if (btnMarkDelivered != null)
                    btnMarkDelivered.setOnClickListener(v ->
                            new AlertDialog.Builder(this)
                                    .setTitle("✅ Confirm Delivery")
                                    .setMessage("Confirm that you have delivered this order and collected payment?")
                                    .setPositiveButton("Yes, Delivered", (d, w) -> {
                                        markAsDelivered(false);
                                        recordEarnings();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show());

            } else {
                // ── PREPAID flow ──────────────────────────────────────────
                if (!otpSentToUser) {
                    // OTP not yet generated — show Generate button
                    setVisible(btnSendOtp, true);
                    showHint("🔑 Generate a one-time OTP for the customer to verify delivery");

                    btnSendOtp.setOnClickListener(v -> generateAndSendOtp());

                } else {
                    // OTP already in Firebase — show Enter OTP button
                    setVisible(btnEnterOtp, true);
                    showHint("🔐 Ask the customer for their OTP to complete delivery");

                    btnEnterOtp.setOnClickListener(v -> {
                        // Launch the professional OTP verify screen
                        Intent intent = new Intent(this, OtpVerifyActivity.class);
                        intent.putExtra("orderId", orderId);
                        startActivityForResult(intent, 101);
                    });
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 4 — Already Delivered (read-only)
        // ═══════════════════════════════════════════════════════════════════
        else if (Constants.STATUS_DELIVERED.equals(status)) {
            showHint("✅ This order has been delivered successfully");
        }
    }

    // ── Called from OtpVerifyActivity result ─────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            // OTP verified — mark delivered
            markAsDelivered(true);
            recordEarnings();
        }
    }

    // ── Step 3a: Generate OTP (prepaid) ──────────────────────────────────────

    private void generateAndSendOtp() {
        String newOtp = String.valueOf(1000 + new Random().nextInt(9000));

        new AlertDialog.Builder(this)
                .setTitle("🔑 Generate Delivery OTP")
                .setMessage("This will generate a unique 4-digit OTP.\n" +
                        "The customer will see it in their Order Tracking screen.\n\nProceed?")
                .setPositiveButton("Generate & Send", (d, w) -> {
                    Map<String, Object> u = new HashMap<>();
                    u.put("deliveryOtp", newOtp);
                    u.put("otpVerified", false);
                    dbRef.child(Constants.NODE_ORDERS).child(orderId).updateChildren(u)
                            .addOnSuccessListener(task -> {
                                otpSentToUser = true;
                                Toast.makeText(this,
                                        "OTP sent to customer! Ask them to share it.",
                                        Toast.LENGTH_LONG).show();
                                // Firebase listener will refresh UI automatically
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed to send OTP. Try again.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Step 4: Mark Delivered ────────────────────────────────────────────────

    private void markAsDelivered(boolean otpVerified) {
        Map<String, Object> u = new HashMap<>();
        u.put("status", Constants.STATUS_DELIVERED);
        u.put("deliveredAt", System.currentTimeMillis());
        if (currentOrder.isCOD()) u.put("paymentStatus", Constants.PAYMENT_PAID);
        if (otpVerified) u.put("otpVerified", true);
        writeOrderUpdates(u);
        Toast.makeText(this, "Order Delivered! ✅", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ── UPI QR bottom sheet ───────────────────────────────────────────────────

    private void showUpiQrBottomSheet() {
        BottomSheetDialog sheet     = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View              sheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottomsheet_upi_qr, null);
        sheet.setContentView(sheetView);

        ImageView      ivQrCode   = sheetView.findViewById(R.id.ivQrCode);
        TextView       tvQrAmount = sheetView.findViewById(R.id.tvQrAmount);
        TextView       tvQrUpiId  = sheetView.findViewById(R.id.tvQrUpiId);
        MaterialButton btnUpiPaid = sheetView.findViewById(R.id.btnUpiPaid);
        MaterialButton btnClose   = sheetView.findViewById(R.id.btnUpiClose);
        MaterialButton btnOpenUpi = sheetView.findViewById(R.id.btnOpenUpiApp); // optional button

        double amount = currentOrder.getTotalAmount();
        // ── ALWAYS use Dev's personal UPI VPA ──
        String vpa    = "vashidevraj50@oksbi";

        if (tvQrAmount != null) tvQrAmount.setText(String.format("₹%.2f", amount));
        if (tvQrUpiId  != null) tvQrUpiId.setText(vpa);

        // Build UPI deep-link — opens any UPI app (GPay, PhonePe, Paytm, etc.)
        String upiUri = "upi://pay?pa=" + vpa
                + "&pn=Dev+Food+App"
                + "&am=" + String.format(Locale.US, "%.2f", amount)
                + "&cu=INR"
                + "&tn=FoodOrder-" + currentOrder.getShortOrderId();

        // Generate QR from UPI URI
        Bitmap qr = generateQrBitmap(upiUri, 600);
        if (ivQrCode != null && qr != null) ivQrCode.setImageBitmap(qr);

        // "Open UPI App" button — fires the deep-link directly
        if (btnOpenUpi != null) {
            btnOpenUpi.setVisibility(View.VISIBLE);
            btnOpenUpi.setOnClickListener(v -> {
                try {
                    Intent upiIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(upiUri));
                    upiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(Intent.createChooser(upiIntent, "Pay with UPI"));
                } catch (Exception e) {
                    Toast.makeText(this,
                            "No UPI app found. Ask customer to scan the QR.", Toast.LENGTH_LONG).show();
                }
            });
        }

        // "Paid" confirmation
        if (btnUpiPaid != null) {
            btnUpiPaid.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("✅ Confirm UPI Payment")
                            .setMessage(String.format("Confirm ₹%.2f received via UPI to %s?", amount, vpa))
                            .setPositiveButton("Yes, Received", (d, w) -> {
                                sheet.dismiss();
                                Map<String, Object> u = new HashMap<>();
                                u.put("paymentMethod", "UPI");
                                u.put("paymentStatus", Constants.PAYMENT_PAID);
                                u.put("codCollected",  true);
                                writeOrderUpdates(u);
                                Toast.makeText(this, "UPI payment recorded ✅", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Not yet", null)
                            .show());
        }
        if (btnClose != null) btnClose.setOnClickListener(v -> sheet.dismiss());
        sheet.show();
    }

    private Bitmap generateQrBitmap(String content, int sizePx) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix    matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
            Bitmap bmp = Bitmap.createBitmap(
                    matrix.getWidth(), matrix.getHeight(), Bitmap.Config.RGB_565);
            for (int x = 0; x < matrix.getWidth(); x++)
                for (int y = 0; y < matrix.getHeight(); y++)
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            return bmp;
        } catch (WriterException e) { return null; }
    }

    // ── Firebase write helper ─────────────────────────────────────────────────

    /**
     * Writes updates to three places simultaneously:
     *  1. Orders/{orderId}                                  ← admin reads
     *  2. Users/{uid}/myOrders/{orderId}                    ← customer sees status
     *  3. DeliveryBoys/{uid}/assignedOrders/{orderId}       ← delivery boy's copy
     */
    private void writeOrderUpdates(Map<String, Object> updates) {
        dbRef.child(Constants.NODE_ORDERS).child(orderId).updateChildren(updates);
        if (currentOrder != null && currentOrder.getUserId() != null)
            dbRef.child(Constants.NODE_USERS).child(currentOrder.getUserId())
                    .child(Constants.NODE_MY_ORDERS).child(orderId).updateChildren(updates);
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(deliveryBoyUid)
                .child(Constants.NODE_ASSIGNED_ORDERS).child(orderId).updateChildren(updates);
    }

    // ── Earnings recording ────────────────────────────────────────────────────

    private void recordEarnings() {
        double commission   = Constants.BASE_DELIVERY_FEE +
                (currentOrder.getTotalAmount() * Constants.DELIVERY_COMMISSION_PERCENT / 100.0);
        double totalEarning = commission + currentOrder.getTipAmount();
        String earningId    = dbRef.child(Constants.NODE_DELIVERY_BOYS).child(deliveryBoyUid)
                .child(Constants.NODE_EARNING_HISTORY).push().getKey();
        if (earningId == null) return;

        String dateStr = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date());
        Map<String, Object> earning = new HashMap<>();
        earning.put("earningId",      earningId);
        earning.put("orderId",        orderId);
        earning.put("restaurantName", currentOrder.getRestaurantName());
        earning.put("customerName",   currentOrder.getUserName());
        earning.put("orderAmount",    currentOrder.getTotalAmount());
        earning.put("tipAmount",      currentOrder.getTipAmount());
        earning.put("commission",     commission);
        earning.put("totalEarning",   totalEarning);
        earning.put("timestamp",      System.currentTimeMillis());
        earning.put("date",           dateStr);
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(deliveryBoyUid)
                .child(Constants.NODE_EARNING_HISTORY).child(earningId).setValue(earning);

        double finalEarning = totalEarning;
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(deliveryBoyUid)
                .child(Constants.NODE_PROFILE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        Double  total = snap.child("totalEarnings").getValue(Double.class);
                        Double  today = snap.child("todayEarnings").getValue(Double.class);
                        Double  month = snap.child("monthEarnings").getValue(Double.class);
                        Integer deliv = snap.child("totalDeliveries").getValue(Integer.class);
                        Map<String, Object> stats = new HashMap<>();
                        stats.put("totalEarnings",   (total != null ? total : 0) + finalEarning);
                        stats.put("todayEarnings",   (today != null ? today : 0) + finalEarning);
                        stats.put("monthEarnings",   (month != null ? month : 0) + finalEarning);
                        stats.put("totalDeliveries", (deliv != null ? deliv : 0) + 1);
                        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(deliveryBoyUid)
                                .child(Constants.NODE_PROFILE).updateChildren(stats);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        Toast.makeText(this,
                String.format("Delivery complete! Earned ₹%.0f 🎉", finalEarning),
                Toast.LENGTH_LONG).show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setVisible(View v, boolean visible) {
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void showHint(String msg) {
        if (tvStatusHint != null) {
            tvStatusHint.setVisibility(View.VISIBLE);
            tvStatusHint.setText(msg);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null)
            dbRef.child(Constants.NODE_ORDERS).child(orderId)
                    .removeEventListener(orderListener);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}