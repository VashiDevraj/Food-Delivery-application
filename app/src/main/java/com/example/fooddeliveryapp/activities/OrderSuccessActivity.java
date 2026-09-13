package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class OrderSuccessActivity extends AppCompatActivity {

    private String orderId, paymentMethod, paymentStatus, restaurantId;
    private double totalAmount, discount;

    private DatabaseReference dbRef;
    private SessionManager    sessionManager;
    private MediaPlayer       mediaPlayer;

    // Views
    private TextView tvSuccessOrderId, tvDeliveryEta, tvSuccessRestaurantName;
    private TextView tvBillTotal, tvBillDiscount, tvBillPaymentMethod, tvBillAmountPaid;
    private LinearLayout llOrderedItems, llFoodImages, layoutBillDiscount;
    private View btnDownloadBill, btnGoHome, btnViewOrders;

    // Order data we load from Firebase
    private String restaurantNameLoaded = "";
    private StringBuilder billText = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        sessionManager = new SessionManager(this);
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        orderId        = getIntent().getStringExtra("orderId");
        totalAmount    = getIntent().getDoubleExtra("totalAmount", 0);
        discount       = getIntent().getDoubleExtra("discount", 0);
        paymentMethod  = getIntent().getStringExtra("paymentMethod");
        paymentStatus  = getIntent().getStringExtra("paymentStatus");
        restaurantId   = getIntent().getStringExtra("restaurantId");
        if (orderId       == null) orderId       = "";
        if (paymentMethod == null) paymentMethod = "COD";
        if (paymentStatus == null) paymentStatus = "Pending";
        if (restaurantId  == null) restaurantId  = "";

        bindViews();
        populateStatic();
        loadRestaurantAndItems();
        playSuccessSound();
        animateTick();

        btnGoHome.setOnClickListener(v -> goHome());
        btnViewOrders.setOnClickListener(v -> {
            startActivity(new Intent(this, OrderHistoryActivity.class));
            finish();
        });
        btnDownloadBill.setOnClickListener(v -> generateAndDownloadPdf());
    }

    private void bindViews() {
        tvSuccessOrderId        = findViewById(R.id.tvSuccessOrderId);
        tvDeliveryEta           = findViewById(R.id.tvDeliveryEta);
        tvSuccessRestaurantName = findViewById(R.id.tvSuccessRestaurantName);
        tvBillTotal             = findViewById(R.id.tvBillTotal);
        tvBillDiscount          = findViewById(R.id.tvBillDiscount);
        tvBillPaymentMethod     = findViewById(R.id.tvBillPaymentMethod);
        tvBillAmountPaid        = findViewById(R.id.tvBillAmountPaid);
        llOrderedItems          = findViewById(R.id.llOrderedItems);
        llFoodImages            = findViewById(R.id.llFoodImages);
        layoutBillDiscount      = findViewById(R.id.layoutBillDiscount);
        btnDownloadBill         = findViewById(R.id.btnDownloadBill);
        btnGoHome               = findViewById(R.id.btnGoHome);
        btnViewOrders           = findViewById(R.id.btnViewOrders);
    }

    private void populateStatic() {
        String shortId = orderId.length() > 8 ? "#" + orderId.substring(orderId.length() - 8).toUpperCase() : "#" + orderId.toUpperCase();
        tvSuccessOrderId.setText(shortId);
        tvBillTotal.setText("₹" + String.format("%.2f", totalAmount));
        tvBillAmountPaid.setText("₹" + String.format("%.2f", totalAmount));
        tvBillPaymentMethod.setText(paymentMethod + " · " + paymentStatus);

        if (discount > 0) {
            layoutBillDiscount.setVisibility(View.VISIBLE);
            tvBillDiscount.setText("-₹" + String.format("%.0f", discount));
        }
    }

    private void loadRestaurantAndItems() {
        if (restaurantId.isEmpty()) return;

        // Load restaurant name
        dbRef.child(Constants.NODE_ADMINS).child(restaurantId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_RESTAURANT_INFO)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        String city = snapshot.child("city").getValue(String.class);
                        String time = snapshot.child("deliveryTime").getValue(String.class);
                        if (name != null) {
                            tvSuccessRestaurantName.setText(name);
                            restaurantNameLoaded = name;
                        }
                        if (time != null) tvDeliveryEta.setText(time);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // Load order items
        dbRef.child(Constants.NODE_ORDERS).child(orderId).child("items")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        llOrderedItems.removeAllViews();
                        llFoodImages.removeAllViews();
                        billText = new StringBuilder();

                        for (DataSnapshot itemSnap : snapshot.getChildren()) {
                            String name     = itemSnap.child("name").getValue(String.class);
                            Long   qty      = itemSnap.child("quantity").getValue(Long.class);
                            Double price    = itemSnap.child("totalPrice").getValue(Double.class);
                            String imgB64   = itemSnap.child("imageBase64").getValue(String.class);

                            if (name == null) continue;
                            int q = qty   != null ? qty.intValue()   : 1;
                            double p = price != null ? price : 0;

                            // Add text row
                            View row = LayoutInflater.from(OrderSuccessActivity.this)
                                    .inflate(R.layout.item_order_success_row, null);
                            if (row != null) {
                                TextView tvName  = row.findViewById(R.id.tvSuccessItemName);
                                TextView tvQty   = row.findViewById(R.id.tvSuccessItemQty);
                                TextView tvPrice = row.findViewById(R.id.tvSuccessItemPrice);
                                if (tvName != null)  tvName.setText(name);
                                if (tvQty != null)   tvQty.setText("×" + q);
                                if (tvPrice != null) tvPrice.setText("₹" + String.format("%.0f", p));
                                llOrderedItems.addView(row);
                            }

                            // Append to bill text
                            billText.append(name).append(" ×").append(q)
                                    .append("  ₹").append(String.format("%.0f", p)).append("\n");

                            // Add food image if available
                            if (imgB64 != null && !imgB64.isEmpty()) {
                                try {
                                    byte[] bytes = Base64.decode(imgB64, Base64.DEFAULT);
                                    Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    if (bmp != null) {
                                        com.google.android.material.card.MaterialCardView card =
                                                new com.google.android.material.card.MaterialCardView(OrderSuccessActivity.this);
                                        int dp64 = (int)(64 * getResources().getDisplayMetrics().density);
                                        int dp8  = (int)(8  * getResources().getDisplayMetrics().density);
                                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp64, dp64);
                                        lp.setMarginEnd(dp8);
                                        card.setLayoutParams(lp);
                                        card.setRadius(dp8);

                                        ImageView iv = new ImageView(OrderSuccessActivity.this);
                                        iv.setLayoutParams(new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                LinearLayout.LayoutParams.MATCH_PARENT));
                                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                        iv.setImageBitmap(bmp);
                                        card.addView(iv);
                                        llFoodImages.addView(card);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void playSuccessSound() {
        try {
            // Use built-in notification sound (no external library needed)
            Uri soundUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
            mediaPlayer = MediaPlayer.create(this, soundUri);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> mp.release());
                mediaPlayer.start();
            }
        } catch (Exception e) {
            // Sound is optional — silently ignore if it fails
        }
    }

    private void animateTick() {
        View tickCard = findViewById(R.id.tvSuccessTick);
        if (tickCard == null) return;
        ScaleAnimation scale = new ScaleAnimation(
                0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(500);
        scale.setInterpolator(new android.view.animation.OvershootInterpolator(1.5f));
        tickCard.startAnimation(scale);
    }

    private void generateAndDownloadPdf() {
        try {
            PdfDocument pdf   = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint titlePaint = new Paint();
            titlePaint.setColor(0xFFE23744);
            titlePaint.setTextSize(24f);
            titlePaint.setFakeBoldText(true);

            Paint bodyPaint = new Paint();
            bodyPaint.setColor(Color.BLACK);
            bodyPaint.setTextSize(14f);

            Paint linePaint = new Paint();
            linePaint.setColor(Color.LTGRAY);
            linePaint.setStrokeWidth(1f);

            int y = 60;

            canvas.drawText("Order Confirmation", 40, y, titlePaint); y += 40;

            bodyPaint.setTextSize(12f);
            bodyPaint.setColor(Color.GRAY);
            String date = new SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()).format(new Date());
            canvas.drawText(date, 40, y, bodyPaint); y += 30;

            canvas.drawLine(40, y, 555, y, linePaint); y += 20;

            bodyPaint.setColor(Color.BLACK);
            bodyPaint.setTextSize(14f);

            String shortId = orderId.length() > 8 ? "#" + orderId.substring(orderId.length() - 8).toUpperCase() : "#" + orderId;
            canvas.drawText("Order ID:    " + shortId, 40, y, bodyPaint); y += 24;
            canvas.drawText("Restaurant:  " + restaurantNameLoaded, 40, y, bodyPaint); y += 24;
            canvas.drawText("Payment:     " + paymentMethod + " (" + paymentStatus + ")", 40, y, bodyPaint); y += 24;

            canvas.drawLine(40, y, 555, y, linePaint); y += 20;

            bodyPaint.setFakeBoldText(true);
            canvas.drawText("ITEMS", 40, y, bodyPaint); y += 24;
            bodyPaint.setFakeBoldText(false);

            for (String line : billText.toString().split("\n")) {
                canvas.drawText(line, 40, y, bodyPaint);
                y += 22;
            }

            y += 10;
            canvas.drawLine(40, y, 555, y, linePaint); y += 20;

            if (discount > 0) {
                bodyPaint.setColor(0xFF4CAF50);
                canvas.drawText("Discount:     -₹" + String.format("%.0f", discount), 40, y, bodyPaint); y += 24;
                bodyPaint.setColor(Color.BLACK);
            }

            bodyPaint.setFakeBoldText(true);
            bodyPaint.setTextSize(16f);
            bodyPaint.setColor(0xFFE23744);
            canvas.drawText("Total Paid:   ₹" + String.format("%.2f", totalAmount), 40, y, bodyPaint);

            pdf.finishPage(page);

            // Save to Downloads
            File dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "Order_" + shortId.replace("#", "") + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            pdf.writeTo(fos);
            pdf.close();
            fos.close();

            Toast.makeText(this, "Bill saved to Downloads 📄", Toast.LENGTH_SHORT).show();

            // Open the PDF
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", file);
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "application/pdf");
            view.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(view, "Open PDF"));

        } catch (Exception e) {
            Toast.makeText(this, "PDF creation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void goHome() {
        Intent intent = new Intent(this, UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        goHome();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}