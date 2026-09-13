package com.example.fooddeliveryapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.CartItem;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * PaymentActivity — Full Razorpay integration + legacy UPI deeplink + COD.
 *
 * Razorpay SDK dependency (add to app/build.gradle):
 *   implementation 'com.razorpay:checkout:1.6.38'
 *
 * AndroidManifest.xml — inside <application>:
 *   <meta-data
 *       android:name="com.razorpay.ApiKey"
 *       android:value="rzp_test_SZ1y8NG8FovTP7" />
 *
 * Proguard (proguard-rules.pro):
 *   -keepclassmembers class * {
 *       @android.webkit.JavascriptInterface <methods>;
 *   }
 *   -keepattributes JavascriptInterface
 *   -keepattributes *Annotation*
 *   -dontwarn com.razorpay.**
 *   -keep class com.razorpay.** {*;}
 *   -optimizations !method/inlining/*
 *   -keepclasseswithmembers class * {
 *       public void onPayment*(...);
 *   }
 */
public class PaymentActivity extends AppCompatActivity implements PaymentResultListener {

    // ── Razorpay ──────────────────────────────────────────────────────────────
    private static final String RAZORPAY_KEY_ID = "rzp_test_SZ1y8NG8FovTP7";

    // ── Legacy UPI request code ───────────────────────────────────────────────
    private static final int REQ_UPI = 200;

    // ── Views ─────────────────────────────────────────────────────────────────
    private RadioGroup        paymentGroup;
    private RadioButton       rbCOD, rbUPI, rbCard;
    private MaterialButton    btnPay, btnConfirmUPI;
    private TextInputEditText etCardNumber, etCardName, etExpiry, etCVV;

    private LinearLayout      btnGooglePay, btnPhonePe, btnOtherUPI, btnCOD, btnCardToggle;
    private LinearLayout      layoutCardForm;
    private MaterialCardView  cardUpiEntry;
    private TextInputEditText etUpiId;
    private MaterialButton    btnVerifyUpi, btnPayNow;
    private TextView          tvPaymentTotal, tvCardToggleArrow;

    // ── Razorpay Pay Buttons ──────────────────────────────────────────────────
    private LinearLayout btnRazorpayCard, btnRazorpayNetBanking, btnRazorpayWallet;

    // ── State ─────────────────────────────────────────────────────────────────
    private String  paymentMethod  = "";
    private String  address        = "";
    private String  phone          = "";
    private String  notes          = "";
    private String  couponCode     = "";
    private String  restaurantId   = "";
    private String  restaurantName = "";
    private String  deliveryNote   = "";
    private double  totalAmount    = 0;
    private double  subtotal       = 0;
    private double  discount       = 0;
    private double  tipAmount      = 0;
    private double  deliveryFee    = 0;
    private boolean cardFormOpen   = false;

    private DatabaseReference dbRef;
    private SessionManager    sessionManager;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Pre-load Razorpay resources for faster checkout open
        Checkout.preload(getApplicationContext());

        dbRef          = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        sessionManager = new SessionManager(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarPayment);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Payment");
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        bindViews();
        readIntentExtras();

        tvPaymentTotal.setText("₹" + String.format("%.2f", totalAmount));
        btnPayNow.setText("₹" + String.format("%.2f", totalAmount) + "  ·  Place Order");

        setupListeners();
    }

    // ── View binding ─────────────────────────────────────────────────────────

    private void bindViews() {
        tvPaymentTotal      = findViewById(R.id.tvPaymentTotal);
        btnGooglePay        = findViewById(R.id.btnGooglePay);
        btnPhonePe          = findViewById(R.id.btnPhonePe);
        btnOtherUPI         = findViewById(R.id.btnOtherUPI);
        btnCOD              = findViewById(R.id.btnCOD);
        btnCardToggle       = findViewById(R.id.btnCardToggle);
        layoutCardForm      = findViewById(R.id.layoutCardForm);
        tvCardToggleArrow   = findViewById(R.id.tvCardToggleArrow);
        cardUpiEntry        = findViewById(R.id.cardUpiEntry);
        etUpiId             = findViewById(R.id.etUpiId);
        btnVerifyUpi        = findViewById(R.id.btnVerifyUpi);
        btnPayNow           = findViewById(R.id.btnPayNow);
        etCardNumber        = findViewById(R.id.etCardNumber);
        etCardName          = findViewById(R.id.etCardName);
        etExpiry            = findViewById(R.id.etExpiry);
        etCVV               = findViewById(R.id.etCVV);
        paymentGroup        = findViewById(R.id.paymentGroup);
        rbCOD               = findViewById(R.id.rbCOD);
        rbUPI               = findViewById(R.id.rbUPI);
        rbCard              = findViewById(R.id.rbCard);
        btnPay              = findViewById(R.id.btnPay);
        btnConfirmUPI       = findViewById(R.id.btnConfirmUPI);

        // Razorpay-powered rows
        btnRazorpayCard        = findViewById(R.id.btnRazorpayCard);
        btnRazorpayNetBanking  = findViewById(R.id.btnRazorpayNetBanking);
        btnRazorpayWallet      = findViewById(R.id.btnRazorpayWallet);
    }

    private void readIntentExtras() {
        address        = safe(getIntent().getStringExtra("address"));
        phone          = safe(getIntent().getStringExtra("phone"));
        notes          = safe(getIntent().getStringExtra("notes"));
        couponCode     = safe(getIntent().getStringExtra("couponCode"));
        restaurantId   = safe(getIntent().getStringExtra("restaurantId"));
        restaurantName = safe(getIntent().getStringExtra("restaurantName"));
        deliveryNote   = safe(getIntent().getStringExtra("deliveryNote"));
        totalAmount    = getIntent().getDoubleExtra("totalAmount", 0);
        subtotal       = getIntent().getDoubleExtra("subtotal",    0);
        discount       = getIntent().getDoubleExtra("discount",    0);
        tipAmount      = getIntent().getDoubleExtra("tipAmount",   0);
        deliveryFee    = getIntent().getDoubleExtra("deliveryFee", 30);
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void setupListeners() {
        // ── Legacy UPI deeplink (Google Pay / PhonePe) ────────────────────
        btnGooglePay.setOnClickListener(v -> launchUpiApp("com.google.android.apps.nbu.paisa.user"));
        btnPhonePe.setOnClickListener(v   -> launchUpiApp("com.phonepe.app"));

        btnOtherUPI.setOnClickListener(v ->
                cardUpiEntry.setVisibility(
                        cardUpiEntry.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));

        btnVerifyUpi.setOnClickListener(v -> {
            String upiId = getText(etUpiId);
            if (upiId.isEmpty() || !upiId.contains("@")) {
                etUpiId.setError("Enter valid UPI ID (e.g. name@upi)");
                return;
            }
            launchUpiDeepLink(upiId);
        });

        // ── Razorpay-powered options ───────────────────────────────────────
        if (btnRazorpayCard != null) {
            btnRazorpayCard.setOnClickListener(v -> {
                paymentMethod = "CARD";
                startRazorpayCheckout("card");
            });
        }
        if (btnRazorpayNetBanking != null) {
            btnRazorpayNetBanking.setOnClickListener(v -> {
                paymentMethod = "NETBANKING";
                startRazorpayCheckout("netbanking");
            });
        }
        if (btnRazorpayWallet != null) {
            btnRazorpayWallet.setOnClickListener(v -> {
                paymentMethod = "WALLET";
                startRazorpayCheckout("wallet");
            });
        }

        // ── Card toggle (legacy — still kept for backward compat) ─────────
        btnCardToggle.setOnClickListener(v -> {
            cardFormOpen = !cardFormOpen;
            layoutCardForm.setVisibility(cardFormOpen ? View.VISIBLE : View.GONE);
            if (tvCardToggleArrow != null) tvCardToggleArrow.setText(cardFormOpen ? "∨" : "›");
            if (cardFormOpen) paymentMethod = "CARD";
        });

        // ── COD ───────────────────────────────────────────────────────────
        btnCOD.setOnClickListener(v -> {
            paymentMethod = "COD";
            fetchPhoneThenPlaceOrder("Pending");
        });

        // ── Bottom "Place Order" button — only used for legacy card form ──
        btnPayNow.setOnClickListener(v -> {
            if ("CARD".equals(paymentMethod) && layoutCardForm.getVisibility() == View.VISIBLE) {
                processLegacyCardPayment();
            } else if (paymentMethod.isEmpty()) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            }
            // For Razorpay paths the button is not the entry point
        });
    }

    // ── Razorpay Checkout ─────────────────────────────────────────────────────

    /**
     * Opens the Razorpay standard checkout.
     *
     * @param preferredMethod  One of: "card", "netbanking", "wallet", "upi", or ""
     *                         When non-empty, Razorpay pre-selects that tab.
     */
    private void startRazorpayCheckout(String preferredMethod) {
        Checkout checkout = new Checkout();
        checkout.setKeyID(RAZORPAY_KEY_ID);
        checkout.setImage(R.mipmap.ic_launcher); // your app logo

        try {
            JSONObject options = new JSONObject();
            options.put("name",        restaurantName.isEmpty() ? "FoodApp" : restaurantName);
            options.put("description", "Order Payment");
            options.put("image",       "https://www.design.com/share/d23a313f-e2a6-486a-ac15-8d67e3168ca6"); // replace with your hosted logo URL

            // Amount in PAISE (multiply rupees × 100)
            options.put("amount", (int) (totalAmount * 100));
            options.put("currency", "INR");

            // Pre-fill customer info
            JSONObject prefill = new JSONObject();
            prefill.put("contact", phone.isEmpty() ? "" : phone);
            String email = sessionManager.getEmail();

            prefill.put("email",
                    (email != null && !email.isEmpty())
                            ? email
                            : "customer@foodapp.com");
            options.put("prefill", prefill);

            // Theme
            JSONObject theme = new JSONObject();
            theme.put("color", "#E23744");
            options.put("theme", theme);

            // Notes stored on Razorpay dashboard
            JSONObject rzpNotes = new JSONObject();
            rzpNotes.put("order_notes",     notes);
            rzpNotes.put("restaurant_id",   restaurantId);
            rzpNotes.put("restaurant_name", restaurantName);
            options.put("notes", rzpNotes);

            // Pre-select payment method tab if specified
            if (!preferredMethod.isEmpty()) {
                JSONObject config = new JSONObject();
                JSONObject display = new JSONObject();
                display.put("defaultblock", preferredMethod);
                config.put("display", display);
                options.put("config", config);
            }

            checkout.open(this, options);

        } catch (JSONException e) {
            Toast.makeText(this, "Payment setup error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ── Razorpay callbacks ────────────────────────────────────────────────────

    /**
     * Called when Razorpay payment succeeds.
     * razorpayPaymentId is the payment ID returned by Razorpay (e.g. pay_XXXXXXXXXX).
     * In production you MUST verify this on your backend server using the order_id + signature.
     */
    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        // Store the Razorpay payment ID for reference
        android.util.Log.d("RAZORPAY", "Payment success: " + razorpayPaymentId);
        Toast.makeText(this, "Payment successful! 🎉", Toast.LENGTH_SHORT).show();
        fetchPhoneThenPlaceOrder("Paid");
    }

    /**
     * Called when Razorpay payment fails or is dismissed.
     */
    @Override
    public void onPaymentError(int code, String description) {
        android.util.Log.e("RAZORPAY", "Payment error " + code + ": " + description);
        if (btnPayNow != null) btnPayNow.setEnabled(true);

        String userMsg;
        switch (code) {
            case Checkout.PAYMENT_CANCELED:
                userMsg = "Payment cancelled. Please try again.";
                break;
            case Checkout.NETWORK_ERROR:
                userMsg = "Network error. Check your connection and retry.";
                break;
            default:
                userMsg = "Payment failed. Please try a different method.";
                break;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Payment Failed")
                .setMessage(userMsg)
                .setPositiveButton("Retry", (d, w) -> {
                    // Re-open checkout with same method
                    startRazorpayCheckout("");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Legacy UPI deeplink (Google Pay / PhonePe direct) ────────────────────

    private void launchUpiApp(String packageName) {
        paymentMethod = "UPI";
        Uri uri = Uri.parse("upi://pay?pa=" + Constants.UPI_VPA
                + "&pn=FoodApp&am=" + totalAmount + "&cu=INR&tn=FoodOrder");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.setPackage(packageName);
        try {
            startActivityForResult(intent, REQ_UPI);
        } catch (android.content.ActivityNotFoundException e) {
            intent.setPackage(null); // fallback to UPI chooser
            startActivityForResult(intent, REQ_UPI);
        }
    }

    private void launchUpiDeepLink(String upiId) {
        paymentMethod = "UPI";
        Uri uri = Uri.parse("upi://pay?pa=" + upiId
                + "&pn=FoodApp&am=" + totalAmount + "&cu=INR&tn=FoodOrder");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        try {
            startActivityForResult(intent, REQ_UPI);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "No UPI app found on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_UPI
                && (resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Payment Status")
                    .setMessage("Did your UPI payment succeed?")
                    .setPositiveButton("Yes, successful", (d, w) -> fetchPhoneThenPlaceOrder("Paid"))
                    .setNegativeButton("No, try again", null)
                    .show();
        }
    }

    // ── Legacy card form (still supported as fallback) ────────────────────────

    private void processLegacyCardPayment() {
        String card   = getText(etCardNumber);
        String name   = getText(etCardName);
        String expiry = getText(etExpiry);
        String cvv    = getText(etCVV);

        if (card.length() < 16)  { etCardNumber.setError("Enter valid 16-digit card number"); return; }
        if (name.isEmpty())      { etCardName.setError("Enter card holder name"); return; }
        if (expiry.length() < 4) { etExpiry.setError("Enter valid expiry MM/YY"); return; }
        if (cvv.length() < 3)    { etCVV.setError("Enter valid CVV"); return; }

        // Route through Razorpay instead of a fake simulation
        startRazorpayCheckout("card");
    }

    // ── Firebase order flow ───────────────────────────────────────────────────

    private void fetchPhoneThenPlaceOrder(String paymentStatus) {
        if (btnPayNow != null) btnPayNow.setEnabled(false);
        String userId = sessionManager.getUid();

        if (!phone.isEmpty()) { fetchCartAndPlaceOrder(paymentStatus, userId, phone); return; }

        String cached = sessionManager.getUserPhone();
        if (cached != null && !cached.isEmpty()) {
            phone = cached;
            fetchCartAndPlaceOrder(paymentStatus, userId, phone);
            return;
        }

        dbRef.child(Constants.NODE_USERS).child(userId).child(Constants.NODE_PROFILE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String fetched = snapshot.child("phone").getValue(String.class);
                        if (fetched != null && !fetched.isEmpty()) {
                            phone = fetched;
                            sessionManager.saveUserPhone(fetched);
                        }
                        if (address.isEmpty()) {
                            String fa = snapshot.child("address").getValue(String.class);
                            if (fa != null) address = fa;
                        }
                        fetchCartAndPlaceOrder(paymentStatus, userId, phone);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        fetchCartAndPlaceOrder(paymentStatus, userId, phone);
                    }
                });
    }

    private void fetchCartAndPlaceOrder(String paymentStatus, String userId, String userPhone) {
        dbRef.child(Constants.NODE_USERS).child(userId).child(Constants.NODE_CART)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> itemsMap         = new HashMap<>();
                        String              cartRestaurantId = restaurantId;

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            CartItem item = snap.getValue(CartItem.class);
                            if (item != null) {
                                item.setFoodId(snap.getKey());
                                if (cartRestaurantId.isEmpty()) {
                                    String rid = snap.child("restaurantId").getValue(String.class);
                                    if (rid != null && !rid.isEmpty()) cartRestaurantId = rid;
                                }
                                Map<String, Object> d = new HashMap<>();
                                d.put("foodId",       item.getFoodId());
                                d.put("name",         item.getName());
                                d.put("price",        item.getPrice());
                                d.put("quantity",     item.getQuantity());
                                d.put("totalPrice",   item.getTotalPrice());
                                d.put("restaurantId", cartRestaurantId);
                                itemsMap.put(snap.getKey(), d);
                            }
                        }

                        if (itemsMap.isEmpty()) {
                            Toast.makeText(PaymentActivity.this, "Cart is empty", Toast.LENGTH_SHORT).show();
                            if (btnPayNow != null) btnPayNow.setEnabled(true);
                            return;
                        }
                        placeOrder(paymentStatus, itemsMap, userId, userPhone, cartRestaurantId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (btnPayNow != null) btnPayNow.setEnabled(true);
                        Toast.makeText(PaymentActivity.this, "Failed to read cart", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void placeOrder(String paymentStatus, Map<String, Object> itemsMap,
                            String userId, String userPhone, String finalRestaurantId) {

        String orderId = dbRef.child(Constants.NODE_ORDERS).push().getKey();
        if (orderId == null) { if (btnPayNow != null) btnPayNow.setEnabled(true); return; }

        String safeNotes     = (notes        != null && !notes.isEmpty())        ? notes        : "";
        String safeDelivNote = (deliveryNote != null && !deliveryNote.isEmpty()) ? deliveryNote : "";
        String safePhone     = (userPhone    != null && !userPhone.isEmpty())    ? userPhone    : "N/A";
        String safeAddress   = (address      != null && !address.isEmpty())      ? address      : "N/A";
        String finalMethod   = paymentMethod.isEmpty() ? "COD" : paymentMethod;

        Map<String, Object> order = new HashMap<>();
        order.put("orderId",        orderId);
        order.put("userId",         userId);
        order.put("restaurantId",   finalRestaurantId);
        order.put("restaurantName", restaurantName);
        order.put("userName",       sessionManager.getName());
        order.put("address",        safeAddress);
        order.put("phone",          safePhone);
        order.put("notes",          safeNotes);
        order.put("deliveryNote",   safeDelivNote);
        order.put("tipAmount",      tipAmount);
        order.put("paymentMethod",  finalMethod);
        order.put("paymentStatus",  paymentStatus);
        order.put("totalAmount",    totalAmount);
        order.put("subtotal",       subtotal);
        order.put("discount",       discount);
        order.put("deliveryFee",    deliveryFee);
        order.put("couponCode",     couponCode.isEmpty() ? "" : couponCode);
        order.put("status",         Constants.STATUS_PENDING);
        order.put("timestamp",      System.currentTimeMillis());
        order.put("reviewed",       false);
        order.put("codCollected",   false);
        order.put("deliveryBoyId",  "");
        order.put("items",          itemsMap);

        dbRef.child(Constants.NODE_ORDERS).child(orderId).setValue(order)
                .addOnSuccessListener(unused -> {
                    dbRef.child(Constants.NODE_USERS).child(userId)
                            .child(Constants.NODE_MY_ORDERS).child(orderId).setValue(order);
                    dbRef.child(Constants.NODE_USERS).child(userId)
                            .child(Constants.NODE_CART).removeValue();

                    Intent intent = new Intent(PaymentActivity.this, OrderSuccessActivity.class);
                    intent.putExtra("orderId",        orderId);
                    intent.putExtra("totalAmount",    totalAmount);
                    intent.putExtra("paymentMethod",  finalMethod);
                    intent.putExtra("paymentStatus",  paymentStatus);
                    intent.putExtra("restaurantId",   finalRestaurantId);
                    intent.putExtra("restaurantName", restaurantName);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (btnPayNow != null) btnPayNow.setEnabled(true);
                    Toast.makeText(this, "Order failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private static String safe(String s) { return s != null ? s : ""; }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}