package com.example.fooddeliveryapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.databinding.ActivityFoodDetailBinding;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * FoodDetailActivity
 *
 * KEY FIX: Now receives "restaurantId" from intent and saves it into CartItem.
 * Without this fix, cart items added from FoodDetailActivity had no restaurantId,
 * causing PaymentActivity to use userId as restaurantId (wrong restaurant bug).
 */
public class FoodDetailActivity extends AppCompatActivity {

    private ActivityFoodDetailBinding binding;
    private DatabaseReference databaseReference;
    private SessionManager sessionManager;

    private int    quantity = 1;
    private String foodId;
    private String foodName;
    private String foodDescription;
    private double foodPrice;
    private String foodImage;
    private String restaurantId; // ← NEW: required to write correct cart item

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFoodDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseReference = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();
        sessionManager = new SessionManager(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        receiveIntentData();
        populateUI();
        setupQuantityButtons();

        binding.btnAddToCart.setOnClickListener(v -> addToCart());
    }

    private void receiveIntentData() {
        foodId          = getIntent().getStringExtra(Constants.EXTRA_FOOD_ID);
        foodName        = getIntent().getStringExtra("foodName");
        foodDescription = getIntent().getStringExtra("foodDescription");
        foodPrice       = getIntent().getDoubleExtra("foodPrice", 0);
        foodImage       = getIntent().getStringExtra("foodImage");
        restaurantId    = getIntent().getStringExtra(Constants.EXTRA_RESTAURANT_ID);
        // Null guard
        if (restaurantId == null) restaurantId = "";
    }

    private void populateUI() {
        binding.tvFoodName.setText(foodName != null ? foodName : "");
        binding.tvFoodDescription.setText(foodDescription != null ? foodDescription : "");
        updatePrice();

        if (foodImage != null && !foodImage.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(foodImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    binding.ivFoodImage.setImageBitmap(bitmap);
                } else {
                    binding.ivFoodImage.setImageResource(R.drawable.ic_food_placeholder);
                }
            } catch (Exception e) {
                binding.ivFoodImage.setImageResource(R.drawable.ic_food_placeholder);
            }
        } else {
            binding.ivFoodImage.setImageResource(R.drawable.ic_food_placeholder);
        }
    }

    private void setupQuantityButtons() {
        binding.btnIncrease.setOnClickListener(v -> {
            quantity++;
            if (quantity > 20) quantity = 20;
            updatePrice();
        });
        binding.btnDecrease.setOnClickListener(v -> {
            quantity--;
            if (quantity < 1) quantity = 1;
            updatePrice();
        });
    }

    private void updatePrice() {
        binding.tvQuantity.setText(String.valueOf(quantity));
        binding.tvUnitPrice.setText("₹" + foodPrice + " per item");
        binding.tvTotalPrice.setText("Total: ₹" + String.format("%.2f", quantity * foodPrice));
    }

    /**
     * ✅ FIXED: Writes restaurantId into cart data.
     * Previous version omitted restaurantId, causing PaymentActivity to
     * fallback to userId for restaurantId — creating phantom restaurants.
     */
    private void addToCart() {
        String userId = sessionManager.getUid();
        if (userId == null || userId.isEmpty() || foodId == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnAddToCart.setEnabled(false);

        DatabaseReference cartRef = databaseReference
                .child(Constants.NODE_USERS)
                .child(userId)
                .child(Constants.NODE_CART)
                .child(foodId);

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Update existing item quantity
                    Long   existing  = snapshot.child("quantity").getValue(Long.class);
                    int    newQty    = (existing != null ? existing.intValue() : 1) + quantity;
                    double newTotal  = foodPrice * newQty;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("quantity",   newQty);
                    updates.put("totalPrice", newTotal);
                    // ✅ Also ensure restaurantId is saved even if updating
                    if (!restaurantId.isEmpty()) {
                        updates.put("restaurantId", restaurantId);
                    }
                    cartRef.updateChildren(updates);
                    Toast.makeText(FoodDetailActivity.this,
                            "Item quantity updated in cart.", Toast.LENGTH_SHORT).show();
                } else {
                    // ✅ New CartItem — write restaurantId field
                    Map<String, Object> cartData = new HashMap<>();
                    cartData.put("foodId",       foodId);
                    cartData.put("name",         foodName   != null ? foodName   : "");
                    cartData.put("price",        foodPrice);
                    cartData.put("quantity",     quantity);
                    cartData.put("totalPrice",   foodPrice * quantity);
                    cartData.put("restaurantId", restaurantId); // ← KEY FIX

                    cartRef.setValue(cartData)
                            .addOnSuccessListener(u ->
                                    Toast.makeText(FoodDetailActivity.this,
                                            foodName + " added to cart", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(FoodDetailActivity.this,
                                            "Failed to add item.", Toast.LENGTH_SHORT).show());
                }

                binding.progressBar.setVisibility(View.GONE);
                binding.btnAddToCart.setEnabled(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnAddToCart.setEnabled(true);
                Toast.makeText(FoodDetailActivity.this,
                        "Failed to add item.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}