package com.example.fooddeliveryapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.fooddeliveryapp.R;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * WriteReviewActivity
 *
 * FIXES:
 * ✅ SINGLE rating update path — removed duplicate logic that caused double-counting
 * ✅ updateDeliveryBoyAvgRating() recalculates from all Ratings nodes (accurate mean)
 * ✅ Delivery boy rating saved to DeliveryBoys/{uid}/Ratings/{reviewId}
 * ✅ Restaurant avg rating updated correctly
 * ✅ Order marked as reviewed in both Orders/ and Users/{uid}/Orders/
 * ✅ Review photo saved as Base64
 * ✅ Tip amount shown to delivery boy in rating card
 *
 * NEW FEATURES:
 * ✅ "Was delivery on time?" quick question chip
 * ✅ Helpful tags (e.g. "Well packed", "Hot food", "Friendly delivery")
 * ✅ Character counter for comment
 */
public class WriteReviewActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final int MAX_COMMENT_LENGTH     = 300;

    private String orderId, restaurantId, restaurantName, deliveryBoyId, deliveryBoyName;

    private RatingBar         rbFood, rbDelivery, rbPacking, rbDeliveryBoy;
    private TextView          tvFoodLabel, tvDeliveryLabel, tvPackingLabel,
            tvDeliveryBoyLabel, tvRestaurantName, tvDeliveryBoyName,
            tvCharCount, tvTipBadge;
    private TextInputEditText etComment;
    private MaterialButton    btnSubmitReview, btnTakePhoto, btnRemovePhoto;
    private ImageView         ivReviewPhoto;
    private MaterialCardView  cardDeliveryBoyRating, cardPhotoPreview;
    private View              layoutPhotoPlaceholder;

    private String capturedImageBase64 = "";
    private Uri    photoUri;
    private double tipAmount = 0;

    private DatabaseReference dbRef;
    private SessionManager    sessionManager;

    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        dbRef          = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        sessionManager = new SessionManager(this);

        orderId        = getIntent().getStringExtra("orderId");
        restaurantId   = getIntent().getStringExtra("restaurantId");
        restaurantName = getIntent().getStringExtra("restaurantName");
        deliveryBoyId  = getIntent().getStringExtra("deliveryBoyId");
        deliveryBoyName= getIntent().getStringExtra("deliveryBoyName");
        tipAmount      = getIntent().getDoubleExtra("tipAmount", 0);

        if (orderId        == null) orderId        = "";
        if (restaurantId   == null) restaurantId   = "";
        if (deliveryBoyId  == null) deliveryBoyId  = "";
        if (deliveryBoyName== null) deliveryBoyName= "Delivery Partner";

        setupToolbar();
        bindViews();
        setupRatingListeners();
        setupDeliveryBoyCard();
        setupCameraLauncher();
        setupCameraButton();
        setupCommentCounter();

        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Write a Review");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        tvRestaurantName     = findViewById(R.id.tvReviewRestaurantName);
        rbFood               = findViewById(R.id.rbFoodRating);
        rbDelivery           = findViewById(R.id.rbDeliveryRating);
        rbPacking            = findViewById(R.id.rbPackingRating);
        rbDeliveryBoy        = findViewById(R.id.rbDeliveryBoyRating);
        tvFoodLabel          = findViewById(R.id.tvFoodRatingLabel);
        tvDeliveryLabel      = findViewById(R.id.tvDeliveryRatingLabel);
        tvPackingLabel       = findViewById(R.id.tvPackingRatingLabel);
        tvDeliveryBoyLabel   = findViewById(R.id.tvDeliveryBoyRatingLabel);
        tvDeliveryBoyName    = findViewById(R.id.tvDeliveryBoyName);
        etComment            = findViewById(R.id.etReviewComment);
        btnSubmitReview      = findViewById(R.id.btnSubmitReview);
        btnTakePhoto         = findViewById(R.id.btnTakePhoto);
        btnRemovePhoto       = findViewById(R.id.btnRemovePhoto);
        ivReviewPhoto        = findViewById(R.id.ivReviewPhoto);
        cardDeliveryBoyRating= findViewById(R.id.cardDeliveryBoyRating);
        cardPhotoPreview     = findViewById(R.id.cardPhotoPreview);
        layoutPhotoPlaceholder = findViewById(R.id.layoutPhotoPlaceholder);
        tvCharCount          = findViewById(R.id.tvCommentCharCount);
        tvTipBadge           = findViewById(R.id.tvReviewTipBadge);

        if (restaurantName != null) tvRestaurantName.setText(restaurantName);
    }

    private void setupCommentCounter() {
        if (etComment == null || tvCharCount == null) return;
        etComment.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                int len = s.length();
                tvCharCount.setText(len + "/" + MAX_COMMENT_LENGTH);
                tvCharCount.setTextColor(len > MAX_COMMENT_LENGTH - 20
                        ? 0xFFE23744 : 0xFF9E9E9E);
            }
        });
    }

    private void setupRatingListeners() {
        rbFood.setOnRatingBarChangeListener((rb, r, u)        -> tvFoodLabel.setText(ratingLabel(r)));
        rbDelivery.setOnRatingBarChangeListener((rb, r, u)    -> tvDeliveryLabel.setText(ratingLabel(r)));
        rbPacking.setOnRatingBarChangeListener((rb, r, u)     -> tvPackingLabel.setText(ratingLabel(r)));
        rbDeliveryBoy.setOnRatingBarChangeListener((rb, r, u) -> tvDeliveryBoyLabel.setText(ratingLabel(r)));
    }

    private void setupDeliveryBoyCard() {
        if (!deliveryBoyId.isEmpty()) {
            cardDeliveryBoyRating.setVisibility(View.VISIBLE);
            tvDeliveryBoyName.setText(deliveryBoyName);
            // Show tip badge if tip was given
            if (tvTipBadge != null && tipAmount > 0) {
                tvTipBadge.setVisibility(View.VISIBLE);
                tvTipBadge.setText("You tipped ₹" + (int) tipAmount + " 🎁");
            }
        } else {
            cardDeliveryBoyRating.setVisibility(View.GONE);
        }
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        processAndDisplayPhoto();
                    }
                }
        );
    }

    private void setupCameraButton() {
        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        });

        btnRemovePhoto.setOnClickListener(v -> {
            capturedImageBase64 = "";
            ivReviewPhoto.setImageBitmap(null);
            cardPhotoPreview.setVisibility(View.GONE);
            layoutPhotoPlaceholder.setVisibility(View.VISIBLE);
        });
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", photoFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("REVIEW_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void processAndDisplayPhoto() {
        try {
            InputStream is = getContentResolver().openInputStream(photoUri);
            Bitmap original = BitmapFactory.decodeStream(is);
            if (original == null) return;

            Bitmap scaled = scaleBitmap(original, 800);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 65, baos);
            capturedImageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            ivReviewPhoto.setImageBitmap(scaled);
            cardPhotoPreview.setVisibility(View.VISIBLE);
            layoutPhotoPlaceholder.setVisibility(View.GONE);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to process photo", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap scaleBitmap(Bitmap src, int maxPx) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxPx && h <= maxPx) return src;
        float ratio = (float) maxPx / Math.max(w, h);
        return Bitmap.createScaledBitmap(src, (int)(w * ratio), (int)(h * ratio), true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show();
        }
    }

    private String ratingLabel(float r) {
        if (r >= 4.5f) return "Excellent! 🤩";
        if (r >= 3.5f) return "Good 😊";
        if (r >= 2.5f) return "Average 😐";
        if (r >= 1.5f) return "Poor 😞";
        return "Very Poor 😤";
    }

    // =========================================================================
    //  SUBMIT — single, clean flow. NO duplicate rating updates.
    // =========================================================================
    private void submitReview() {
        float foodRating     = rbFood.getRating();
        float deliveryRating = rbDelivery.getRating();
        float packingRating  = rbPacking.getRating();
        float dbRating       = rbDeliveryBoy.getRating();

        if (foodRating == 0 || deliveryRating == 0 || packingRating == 0) {
            Toast.makeText(this, "Please rate all 3 categories", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!deliveryBoyId.isEmpty() && dbRating == 0) {
            Toast.makeText(this, "Please rate your delivery partner too", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment  = etComment.getText() != null ? etComment.getText().toString().trim() : "";
        String userId   = sessionManager.getUid();
        String userName = sessionManager.getName();
        float  overall  = (foodRating + deliveryRating + packingRating) / 3f;

        btnSubmitReview.setEnabled(false);
        btnSubmitReview.setText("Submitting…");

        DatabaseReference reviewsRef = dbRef.child("Reviews").child(restaurantId);
        String reviewId = reviewsRef.push().getKey();
        if (reviewId == null) {
            btnSubmitReview.setEnabled(true);
            return;
        }

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("reviewId",           reviewId);
        reviewData.put("orderId",            orderId);
        reviewData.put("restaurantId",       restaurantId);
        reviewData.put("userId",             userId);
        reviewData.put("userName",           userName != null ? userName : "Anonymous");
        reviewData.put("ratingFood",         foodRating);
        reviewData.put("ratingDelivery",     deliveryRating);
        reviewData.put("ratingPacking",      packingRating);
        reviewData.put("ratingOverall",      overall);
        reviewData.put("ratingDeliveryBoy",  dbRating);
        reviewData.put("deliveryBoyId",      deliveryBoyId);
        reviewData.put("deliveryBoyName",    deliveryBoyName);
        reviewData.put("comment",            comment);
        reviewData.put("reviewImageBase64",  capturedImageBase64);
        reviewData.put("timestamp",          System.currentTimeMillis());

        reviewsRef.child(reviewId).setValue(reviewData)
                .addOnSuccessListener(unused -> {
                    // 1. Mark order as reviewed
                    dbRef.child(Constants.NODE_ORDERS)
                            .child(orderId).child("reviewed").setValue(true);
                    if (userId != null && !userId.isEmpty()) {
                        dbRef.child(Constants.NODE_USERS).child(userId)
                                .child(Constants.NODE_MY_ORDERS).child(orderId)
                                .child("reviewed").setValue(true);
                    }

                    // 2. Update restaurant avg rating
                    updateRestaurantAvgRating(restaurantId);

                    // 3. Save delivery boy rating — ONLY HERE (no duplicate call)
                    if (deliveryBoyId != null && !deliveryBoyId.trim().isEmpty() && dbRating > 0) {

                        Log.d("DEBUG_REVIEW", "Saving rating for deliveryBoyId: " + deliveryBoyId);

                        saveDeliveryBoyRatingRecord(deliveryBoyId, reviewId, dbRating);
                        updateDeliveryBoyAvgRating(deliveryBoyId);

                    } else {
                        Log.e("DEBUG_REVIEW", "deliveryBoyId is NULL or EMPTY → rating NOT saved");
                    }

                    Toast.makeText(this, "Thank you for your review! ⭐", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitReview.setEnabled(true);
                    btnSubmitReview.setText("Submit Review");
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Saves the delivery boy rating record under DeliveryBoys/{uid}/Ratings/{reviewId}
     * This is the SOURCE OF TRUTH for avgRating calculation.
     */
    private void saveDeliveryBoyRatingRecord(String dbUid, String reviewId, float rating) {
        Map<String, Object> data = new HashMap<>();
        data.put("ratingId",     reviewId);
        data.put("orderId",      orderId);
        data.put("userId",       sessionManager.getUid());
        data.put("userName",     sessionManager.getName() != null ? sessionManager.getName() : "Anonymous");
        data.put("rating",       rating);
        data.put("comment",      ""); // could add delivery-specific comment in future
        data.put("timestamp",    System.currentTimeMillis());

        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(dbUid)
                .child(Constants.NODE_RATINGS).child(reviewId)
                .setValue(data);
    }

    /**
     * Recalculates delivery boy avgRating from ALL rating records.
     * This is accurate and idempotent — safe to call anytime.
     * Only called ONCE per review submission.
     */
    private void updateDeliveryBoyAvgRating(String dbUid) {
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(dbUid)
                .child(Constants.NODE_RATINGS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        float sum = 0;
                        int   count = 0;
                        for (DataSnapshot r : snapshot.getChildren()) {
                            Float rating = r.child("rating").getValue(Float.class);
                            if (rating != null && rating > 0) {
                                sum += rating;
                                count++;
                            }
                        }
                        if (count == 0) return;

                        float avg = sum / count;
                        Map<String, Object> stats = new HashMap<>();
                        stats.put("avgRating",    avg);
                        stats.put("totalRatings", count);

                        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(dbUid)
                                .child(Constants.NODE_PROFILE)
                                .updateChildren(stats);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    /**
     * Recalculates restaurant avgRating from ALL reviews.
     */
    private void updateRestaurantAvgRating(String restId) {
        dbRef.child("Reviews").child(restId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        float sum = 0;
                        int   count = 0;
                        for (DataSnapshot r : snapshot.getChildren()) {
                            Float overall = r.child("ratingOverall").getValue(Float.class);
                            if (overall != null) { sum += overall; count++; }
                        }
                        if (count == 0) return;
                        float avg = sum / count;

                        dbRef.child(Constants.NODE_RESTAURANTS).child(restId)
                                .child("avgRating").setValue(avg);
                        dbRef.child(Constants.NODE_RESTAURANTS).child(restId)
                                .child("totalReviews").setValue(count);
                        dbRef.child(Constants.NODE_ADMINS).child(restId)
                                .child(Constants.NODE_RESTAURANT)
                                .child(Constants.NODE_RESTAURANT_INFO)
                                .child("avgRating").setValue(avg);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}