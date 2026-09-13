package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.databinding.ActivityRestaurantSetupBinding;
import com.example.fooddeliveryapp.models.Restaurant;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;

/**
 * RestaurantSetupActivity
 *
 * First-time setup for admin restaurant.
 * Delivery time is auto-calculated (15-30 min range, randomized).
 * Rating starts at 0 and grows from real orders.
 * Image: gallery OR camera.
 */
public class RestaurantSetupActivity extends AppCompatActivity {

    private ActivityRestaurantSetupBinding binding;
    private DatabaseReference dbRef;
    private SessionManager    sessionManager;

    private Bitmap selectedImageBitmap = null;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding        = ActivityRestaurantSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        registerImageLaunchers();
        setupImagePicker();

        binding.btnSaveRestaurant.setOnClickListener(v -> attemptSave());
    }

    // ── Image Launchers ───────────────────────────────────────────────────────

    private void registerImageLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        loadImageFromUri(result.getData().getData());
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap photo = (Bitmap) extras.get("data");
                            if (photo != null) setSelectedImage(photo);
                        }
                    }
                });
    }

    private void setupImagePicker() {
        binding.cardImagePicker.setOnClickListener(v -> showImageSourceDialog());
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Add Restaurant Photo")
                .setItems(new String[]{"📷  Take Photo", "🖼️  Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (intent.resolveActivity(getPackageManager()) != null)
                            cameraLauncher.launch(intent);
                        else
                            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(intent);
                    }
                }).show();
    }

    private void loadImageFromUri(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            setSelectedImage(BitmapFactory.decodeStream(stream));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void setSelectedImage(Bitmap bitmap) {
        selectedImageBitmap = bitmap;
        binding.ivRestaurantImage.setImageBitmap(bitmap);
        binding.tvPickImage.setVisibility(View.GONE);
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void attemptSave() {
        String name    = binding.etRestaurantName.getText().toString().trim();
        String city    = binding.etCity.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String pincode = binding.etPincode.getText().toString().trim();
        String phone   = binding.etPhone.getText().toString().trim();
        boolean vegOnly = binding.switchVegOnly.isChecked();

        if (TextUtils.isEmpty(name))    { binding.etRestaurantName.setError("Required"); return; }
        if (TextUtils.isEmpty(city))    { binding.etCity.setError("Required");           return; }
        if (TextUtils.isEmpty(address)) { binding.etAddress.setError("Required");        return; }
        if (TextUtils.isEmpty(pincode)) { binding.etPincode.setError("Required");        return; }

        // Compress image
        String imageBase64Temp = "";

        if (selectedImageBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            imageBase64Temp = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        }

        final String imageBase64 = imageBase64Temp;

        showLoading(true);
        String adminUid = sessionManager.getUid();

        // Count existing orders to set delivery time dynamically
        dbRef.child(Constants.NODE_ORDERS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long orderCount = 0;
                for (DataSnapshot o : snapshot.getChildren()) {
                    String rid = o.child("restaurantId").getValue(String.class);
                    if (adminUid.equals(rid)) orderCount++;
                }
                String deliveryTime = calculateDeliveryTime(orderCount);
                saveRestaurant(adminUid, name, city, address, pincode, phone,
                        imageBase64, vegOnly, deliveryTime);
            }
            @Override public void onCancelled(DatabaseError error) {
                // Fallback to default if orders can't be read
                saveRestaurant(adminUid, name, city, address, pincode, phone,
                        imageBase64, vegOnly, "20-30 min");
            }
        });
    }

    /**
     * Delivery time is randomized between 15-30 min.
     * Higher order count = slightly longer range (busier restaurant).
     */
    private String calculateDeliveryTime(long orderCount) {
        Random random = new Random();
        int base = (orderCount > 50) ? 20 : 15;   // busier = starts at 20
        int min  = base + random.nextInt(5);        // e.g. 15-19 or 20-24
        int max  = min + 10 + random.nextInt(6);    // always 10-15 min window
        return min + "-" + max + " min";
    }

    private void saveRestaurant(String adminUid, String name, String city, String address,
                                String pincode, String phone, String imageBase64,
                                boolean vegOnly, String deliveryTime) {
        Restaurant restaurant = new Restaurant();

// ✅ USE THIS (already available)
        restaurant.setRestaurantId(adminUid);   // ID = admin UID

        restaurant.setName(name);
        restaurant.setCity(city);
        restaurant.setAddress(address);
        restaurant.setPincode(pincode);

        restaurant.setImageBase64(imageBase64); // ✔ already created above
        restaurant.setRating(4.0);              // ✔ default rating

        restaurant.setDeliveryTime(deliveryTime); // ✔ already stored

        restaurant.setPhone(phone);
        restaurant.setVegOnly(vegOnly);

        dbRef.child(Constants.NODE_ADMINS)
                .child(adminUid)
                .child(Constants.NODE_RESTAURANT)
                .child(Constants.NODE_RESTAURANT_INFO)
                .setValue(restaurant)
                .addOnSuccessListener(unused -> {
                    dbRef.child(Constants.NODE_RESTAURANTS).child(adminUid).setValue(restaurant);
                    showLoading(false);
                    sessionManager.setRestaurantSetupDone();
                    Toast.makeText(this, "Restaurant saved! ✅", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveRestaurant.setEnabled(!loading);
    }

    @Override
    public void onBackPressed() {
        // Do nothing — admin must complete setup
    }
}