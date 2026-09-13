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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.databinding.ActivityRestaurantEditBinding;
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

/**
 * RestaurantEditActivity
 *
 * Allows admin to edit their existing restaurant info.
 * Pre-fills all fields with current Firebase data.
 * Launched from AdminDashboardActivity "Edit Restaurant Info" button.
 */
public class RestaurantEditActivity extends AppCompatActivity {

    private ActivityRestaurantEditBinding binding;
    private DatabaseReference dbRef;
    private SessionManager    sessionManager;
    private String            adminUid;

    private Bitmap selectedImageBitmap = null;
    private String existingImageBase64 = "";   // keep old image if no new one picked
    private String existingDeliveryTime = "20-30 min";

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding        = ActivityRestaurantEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        adminUid       = sessionManager.getUid();
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        // Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Restaurant Info");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        registerImageLaunchers();
        setupImagePicker();
        loadExistingData();   // ← pre-fill all fields

        binding.btnUpdateRestaurant.setOnClickListener(v -> attemptUpdate());
    }

    // ── Load existing restaurant data ─────────────────────────────────────────

    private void loadExistingData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnUpdateRestaurant.setEnabled(false);

        dbRef.child(Constants.NODE_ADMINS)
                .child(adminUid)
                .child(Constants.NODE_RESTAURANT)
                .child(Constants.NODE_RESTAURANT_INFO)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnUpdateRestaurant.setEnabled(true);

                        if (!snapshot.exists()) return;
                        Restaurant r = snapshot.getValue(Restaurant.class);
                        if (r == null) return;

                        // Pre-fill all fields
                        binding.etRestaurantName.setText(r.getName());
                        binding.etCity.setText(r.getCity());
                        binding.etAddress.setText(r.getAddress());
                        binding.etPincode.setText(r.getPincode());
                        binding.etPhone.setText(r.getPhone());
                        binding.switchVegOnly.setChecked(r.isVegOnly());

                        existingDeliveryTime = r.getDeliveryTime();

                        // Load existing image
                        existingImageBase64 = r.getImageBase64();
                        if (existingImageBase64 != null && !existingImageBase64.isEmpty()) {
                            try {
                                byte[] bytes = Base64.decode(existingImageBase64, Base64.DEFAULT);
                                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                binding.ivRestaurantImage.setImageBitmap(bmp);
                                binding.tvPickImage.setVisibility(View.GONE);
                            } catch (Exception ignored) {}
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnUpdateRestaurant.setEnabled(true);
                        Toast.makeText(RestaurantEditActivity.this,
                                "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Image Launchers ───────────────────────────────────────────────────────

    private void registerImageLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null)
                        loadImageFromUri(result.getData().getData());
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
                .setTitle("Change Restaurant Photo")
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

    // ── Update ────────────────────────────────────────────────────────────────

    private void attemptUpdate() {
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

        // Use new image if selected, otherwise keep existing
        String imageBase64 = existingImageBase64;
        if (selectedImageBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        }

        showLoading(true);

        Restaurant restaurant = new Restaurant();

// ✅ USE THIS (already available)
        restaurant.setRestaurantId(adminUid);   // ID = admin UID

        restaurant.setName(name);
        restaurant.setCity(city);
        restaurant.setAddress(address);
        restaurant.setPincode(pincode);

        restaurant.setImageBase64(imageBase64); // ✔ already created above
        restaurant.setRating(4.0);              // ✔ default rating

        restaurant.setDeliveryTime(existingDeliveryTime); // ✔ already stored

        restaurant.setPhone(phone);
        restaurant.setVegOnly(vegOnly);
        // Update both admin's copy and global copy
        dbRef.child(Constants.NODE_ADMINS)
                .child(adminUid)
                .child(Constants.NODE_RESTAURANT)
                .child(Constants.NODE_RESTAURANT_INFO)
                .setValue(restaurant)
                .addOnSuccessListener(unused -> {
                    dbRef.child(Constants.NODE_RESTAURANTS).child(adminUid).setValue(restaurant);
                    showLoading(false);
                    Toast.makeText(this, "Restaurant updated! ✅", Toast.LENGTH_SHORT).show();
                    finish();   // go back to dashboard
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnUpdateRestaurant.setEnabled(!loading);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}