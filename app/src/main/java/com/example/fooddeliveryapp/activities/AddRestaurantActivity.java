package com.example.fooddeliveryapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fooddeliveryapp.adapters.AdminRestaurantAdapter;
import com.example.fooddeliveryapp.databinding.ActivityAddRestaurantBinding;
import com.example.fooddeliveryapp.models.Restaurant;
import com.google.firebase.database.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AddRestaurantActivity extends AppCompatActivity {

    private ActivityAddRestaurantBinding binding;
    private DatabaseReference db;

    private final List<Restaurant> restaurantList = new ArrayList<>();
    private AdminRestaurantAdapter adapter;
    private Bitmap selectedBitmap;

    // ── Image Pickers ─────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        InputStream s = getContentResolver().openInputStream(result.getData().getData());
                        selectedBitmap = BitmapFactory.decodeStream(s);
                        binding.ivRestaurantImagePreview.setImageBitmap(selectedBitmap);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedBitmap = (Bitmap) result.getData().getExtras().get("data");
                    binding.ivRestaurantImagePreview.setImageBitmap(selectedBitmap);
                }
            });

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddRestaurantBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseDatabase.getInstance().getReference();

        setupToolbar();
        setupRecyclerView();
        loadRestaurants();

        binding.btnSelectImage.setOnClickListener(v -> selectImage());
        binding.btnSaveRestaurant.setOnClickListener(v -> saveRestaurant());
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Restaurants");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ── Image selection ───────────────────────────────────────────────────────

    private void selectImage() {
        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(new String[]{"Camera", "Gallery"}, (d, w) -> {
                    if (w == 0) openCamera();
                    else galleryLauncher.launch(new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                }).show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        }
    }

    private String bitmapToBase64(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    // ── Save restaurant ───────────────────────────────────────────────────────

    private void saveRestaurant() {
        String name     = getText(binding.etRestaurantName);
        String city     = getText(binding.etRestaurantCity);
        String address  = getText(binding.etRestaurantAddress);
        String pincode  = getText(binding.etRestaurantPincode);
        String phone    = getText(binding.etRestaurantPhone);
        String delivery = getText(binding.etDeliveryTime);
        String minOrder = getText(binding.etMinOrderAmount);
        String offer    = getText(binding.etRestaurantOffer);
        boolean vegOnly = binding.switchVegOnly.isChecked();

        if (TextUtils.isEmpty(name)) {
            binding.etRestaurantName.setError("Required"); return;
        }
        if (TextUtils.isEmpty(city)) {
            binding.etRestaurantCity.setError("Required"); return;
        }
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show(); return;
        }

        binding.btnSaveRestaurant.setEnabled(false);
        binding.btnSaveRestaurant.setText("Saving...");

        String imageBase64 = bitmapToBase64(selectedBitmap);
        String id          = db.child("restaurants").push().getKey();
        if (id == null) return;

        Restaurant r = new Restaurant();
        r.setRestaurantId(id);
        r.setName(name);
        r.setCity(city);
        r.setAddress(address);
        r.setPincode(pincode);
        // Store phone & delivery separately in a map for flexibility
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        map.put("restaurantId", id);
        map.put("name",         name);
        map.put("city",         city);
        map.put("address",      address);
        map.put("pincode",      pincode);
        map.put("phone",        phone);
        map.put("deliveryTime", delivery);
        map.put("minOrder",     minOrder);
        map.put("offer",        offer);
        map.put("vegOnly",      vegOnly);
        map.put("imageBase64",  imageBase64);
        map.put("rating",       4.0);

        db.child("restaurants").child(id).setValue(map)
                .addOnSuccessListener(u -> {
                    Toast.makeText(this, "Restaurant added ✅", Toast.LENGTH_SHORT).show();
                    clearForm();
                    binding.btnSaveRestaurant.setEnabled(true);
                    binding.btnSaveRestaurant.setText("Save Restaurant");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.btnSaveRestaurant.setEnabled(true);
                    binding.btnSaveRestaurant.setText("Save Restaurant");
                });
    }

    private void clearForm() {
        binding.etRestaurantName.setText("");
        binding.etRestaurantCity.setText("");
        binding.etRestaurantAddress.setText("");
        binding.etRestaurantPincode.setText("");
        binding.etRestaurantPhone.setText("");
        binding.etDeliveryTime.setText("");
        binding.etMinOrderAmount.setText("");
        binding.etRestaurantOffer.setText("");
        binding.switchVegOnly.setChecked(false);
        selectedBitmap = null;
        binding.ivRestaurantImagePreview.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new AdminRestaurantAdapter(this, restaurantList,
                new AdminRestaurantAdapter.OnRestaurantActionListener() {
                    @Override
                    public void onEdit(Restaurant r) {
                        // TODO: open EditRestaurantActivity with extras
                        Toast.makeText(AddRestaurantActivity.this,
                                "Edit: " + r.getName(), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onDelete(Restaurant r) {
                        new AlertDialog.Builder(AddRestaurantActivity.this)
                                .setTitle("Delete Restaurant")
                                .setMessage("Delete \"" + r.getName() + "\"?")
                                .setPositiveButton("Delete", (d, w) ->
                                        db.child("restaurants").child(r.getRestaurantId()).removeValue())
                                .setNegativeButton("Cancel", null).show();
                    }
                });
        binding.rvRestaurantList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRestaurantList.setAdapter(adapter);
    }

    private void loadRestaurants() {
        db.child("restaurants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                restaurantList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Restaurant r = snap.getValue(Restaurant.class);
                    if (r != null) {
                        r.setRestaurantId(snap.getKey());
                        restaurantList.add(r);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}








































































































































































































































































