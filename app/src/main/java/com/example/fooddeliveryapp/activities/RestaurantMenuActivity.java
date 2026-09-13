package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.adapters.FoodAdapter;
import com.example.fooddeliveryapp.adapters.UserRestaurantReviewsAdapter;
import com.example.fooddeliveryapp.databinding.ActivityRestaurantMenuBinding;
import com.example.fooddeliveryapp.models.FoodItem;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RestaurantMenuActivity — Menu tab + Reviews tab
 *
 * ✅ Cart bar at bottom (like user dashboard) — tap to go to CartActivity
 * ✅ Review photos shown (Base64) — shown only when present
 * ✅ Delivery boy rating shown in reviews
 * ✅ Cart writes include restaurantId
 * ✅ Single-restaurant cart enforcement
 */
public class RestaurantMenuActivity extends AppCompatActivity {

    private ActivityRestaurantMenuBinding binding;
    private SessionManager    sessionManager;
    private DatabaseReference dbRef;

    // Menu
    private FoodAdapter       foodAdapter;
    private final List<FoodItem> foodList    = new ArrayList<>();
    private final List<FoodItem> allFoodList = new ArrayList<>();

    // Reviews
    private UserRestaurantReviewsAdapter reviewsAdapter;
    private final List<UserRestaurantReviewsAdapter.ReviewItem> reviewList = new ArrayList<>();

    private String restaurantId;
    private String restaurantName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRestaurantMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        dbRef = FirebaseDatabase
                .getInstance(Constants.FIREBASE_URL)
                .getReference();

        restaurantId   = getIntent().getStringExtra(Constants.EXTRA_RESTAURANT_ID);
        restaurantName = getIntent().getStringExtra(Constants.EXTRA_RESTAURANT_NAME);
        if (restaurantName == null) restaurantName = "Menu";
        if (restaurantId   == null) restaurantId   = "";

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(restaurantName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupTabs();
        setupMenuRecyclerView();
        setupReviewsRecyclerView();
        setupSearch();
        setupCartBar();
        loadMenuFromRestaurantNode();
        loadReviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBar();
    }

    // ── Cart bar ──────────────────────────────────────────────────────────────

    private void setupCartBar() {
        // Tap anywhere on cart bar → open cart
        binding.cardCartBar.setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));
        binding.tvViewCart.setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));
        updateCartBar();
    }

    private void updateCartBar() {
        String userId = sessionManager.getUid();
        if (userId == null) return;
        dbRef.child(Constants.NODE_USERS).child(userId).child(Constants.NODE_CART)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        if (count > 0) {
                            binding.cardCartBar.setVisibility(View.VISIBLE);
                            binding.tvCartItemCount.setText(count + (count == 1 ? " item" : " items") + " added");

                            // Calculate total price
                            double total = 0;
                            for (DataSnapshot item : snapshot.getChildren()) {
                                Double tp = item.child("totalPrice").getValue(Double.class);
                                if (tp != null) total += tp;
                            }
                            binding.tvCartTotal.setText("₹" + String.format("%.0f", total));
                        } else {
                            binding.cardCartBar.setVisibility(View.GONE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🍽️  Menu"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("⭐  Reviews"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.layoutMenuSearch.setVisibility(View.VISIBLE);
                    binding.rvMenuItems.setVisibility(View.VISIBLE);
                    binding.layoutReviewsSection.setVisibility(View.GONE);
                } else {
                    binding.layoutMenuSearch.setVisibility(View.GONE);
                    binding.rvMenuItems.setVisibility(View.GONE);
                    binding.layoutReviewsSection.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private void setupMenuRecyclerView() {
        foodAdapter = new FoodAdapter(this, foodList, new FoodAdapter.OnFoodClickListener() {
            @Override
            public void onFoodClick(FoodItem item) {
                Intent intent = new Intent(RestaurantMenuActivity.this, FoodDetailActivity.class);
                intent.putExtra(Constants.EXTRA_FOOD_ID,          item.getFoodId());
                intent.putExtra("foodName",                       item.getName());
                intent.putExtra("foodDescription",                item.getDescription());
                intent.putExtra("foodPrice",                      item.getPrice());
                intent.putExtra("foodImage",                      item.getImageBase64());
                intent.putExtra("categoryId",                     item.getCategoryId());
                intent.putExtra(Constants.EXTRA_RESTAURANT_ID,   restaurantId);
                startActivity(intent);
            }
            @Override
            public void onAddToCartClick(FoodItem item) { addToCartWithRestaurant(item); }
        });
        binding.rvMenuItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMenuItems.setAdapter(foodAdapter);
    }

    private void setupReviewsRecyclerView() {
        reviewsAdapter = new UserRestaurantReviewsAdapter(this, reviewList);
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviews.setAdapter(reviewsAdapter);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearch() {
        if (binding.etMenuSearch == null) return;
        binding.etMenuSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence query, int i, int b, int c) {
                String q    = query.toString().trim().toLowerCase();
                boolean isVeg = sessionManager.isVegMode();
                foodList.clear();
                for (FoodItem item : allFoodList) {
                    if (isVeg && !item.isVeg()) continue;
                    if (q.isEmpty()
                            || (item.getName() != null && item.getName().toLowerCase().contains(q))
                            || (item.getDescription() != null && item.getDescription().toLowerCase().contains(q)))
                        foodList.add(item);
                }
                foodAdapter.notifyDataSetChanged();
                binding.tvEmpty.setVisibility(foodList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    // ── Load Menu ─────────────────────────────────────────────────────────────

    private void loadMenuFromRestaurantNode() {
        if (restaurantId.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);

        dbRef.child(Constants.NODE_ADMINS).child(restaurantId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_MENU)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        binding.progressBar.setVisibility(View.GONE);
                        allFoodList.clear();
                        foodList.clear();
                        boolean isVeg = sessionManager.isVegMode();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            try {
                                FoodItem food = snap.getValue(FoodItem.class);
                                if (food == null) continue;
                                food.setFoodId(snap.getKey());
                                food.setRestaurantId(restaurantId);
                                if (!food.isAvailable()) continue;
                                allFoodList.add(food);
                                if (!isVeg || food.isVeg()) foodList.add(food);
                            } catch (Exception ignored) {}
                        }
                        foodAdapter.notifyDataSetChanged();
                        binding.tvEmpty.setVisibility(foodList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(RestaurantMenuActivity.this, "Failed to load menu",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Load Reviews ──────────────────────────────────────────────────────────

    private void loadReviews() {
        binding.reviewProgressBar.setVisibility(View.VISIBLE);

        dbRef.child("Reviews").child(restaurantId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        binding.reviewProgressBar.setVisibility(View.GONE);
                        reviewList.clear();

                        float totalRating = 0;
                        int   count       = 0;

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String reviewId     = snap.getKey();
                            String userName     = snap.child("userName").getValue(String.class);
                            String comment      = snap.child("comment").getValue(String.class);
                            Float  rFood        = snap.child("ratingFood").getValue(Float.class);
                            Float  rDelivery    = snap.child("ratingDelivery").getValue(Float.class);
                            Float  rPacking     = snap.child("ratingPacking").getValue(Float.class);
                            Float  rOverall     = snap.child("ratingOverall").getValue(Float.class);
                            Long   ts           = snap.child("timestamp").getValue(Long.class);
                            String orderId      = snap.child("orderId").getValue(String.class);
                            // ✅ Read review image for user side too
                            String imgBase64    = snap.child("reviewImageBase64").getValue(String.class);

                            if (rOverall != null) { totalRating += rOverall; count++; }

                            reviewList.add(new UserRestaurantReviewsAdapter.ReviewItem(
                                    reviewId   != null ? reviewId   : "",
                                    userName   != null ? userName   : "Anonymous",
                                    comment    != null ? comment    : "",
                                    rFood      != null ? rFood      : 0f,
                                    rDelivery  != null ? rDelivery  : 0f,
                                    rPacking   != null ? rPacking   : 0f,
                                    rOverall   != null ? rOverall   : 0f,
                                    ts         != null ? ts         : 0L,
                                    orderId    != null ? orderId    : "",
                                    imgBase64  != null ? imgBase64  : ""
                            ));
                        }

                        // Sort newest first
                        reviewList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                        reviewsAdapter.notifyDataSetChanged();
                        updateReviewSummary(totalRating, count);

                        boolean empty = reviewList.isEmpty();
                        binding.tvNoReviews.setVisibility(empty ? View.VISIBLE : View.GONE);
                        binding.rvReviews.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        binding.reviewProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void updateReviewSummary(float total, int count) {
        if (count == 0) {
            binding.tvReviewSummaryRating.setText("—");
            binding.tvReviewSummaryCount.setText("No reviews yet");
            return;
        }
        float avg = total / count;
        binding.tvReviewSummaryRating.setText(
                String.format(java.util.Locale.getDefault(), "%.1f ⭐", avg));

        int color;
        if (avg >= 3.5f)      color = 0xFF2E7D32;
        else if (avg >= 2.0f) color = 0xFFFF8F00;
        else                  color = 0xFFE23744;
        binding.tvReviewSummaryRating.setTextColor(color);

        binding.tvReviewSummaryCount.setText(count + (count == 1 ? " review" : " reviews"));
    }

    // ── Cart — single restaurant enforcement ─────────────────────────────────

    private void addToCartWithRestaurant(FoodItem food) {
        String userId = sessionManager.getUid();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }
        String foodId = food.getFoodId();
        if (foodId == null || foodId.isEmpty()) return;

        DatabaseReference cartRef = dbRef.child(Constants.NODE_USERS).child(userId)
                .child(Constants.NODE_CART);

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        String existingRid = snap.child("restaurantId").getValue(String.class);
                        if (existingRid != null && !existingRid.equals(restaurantId)) {
                            new androidx.appcompat.app.AlertDialog.Builder(RestaurantMenuActivity.this,
                                    R.style.RoundedAlertDialog)
                                    .setTitle("Replace Cart?")
                                    .setMessage("Your cart has items from another restaurant. "
                                            + "Adding this will clear your current cart.")
                                    .setPositiveButton("Start Fresh 🗑", (d, w) ->
                                            cartRef.removeValue().addOnSuccessListener(u ->
                                                    writeCartItem(food, userId)))
                                    .setNegativeButton("Keep Cart", null)
                                    .show();
                            return;
                        }
                    }
                }
                writeCartItem(food, userId);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void writeCartItem(FoodItem food, String userId) {
        String foodId = food.getFoodId();
        DatabaseReference itemRef = dbRef.child(Constants.NODE_USERS).child(userId)
                .child(Constants.NODE_CART).child(foodId);

        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long qty    = snapshot.child("quantity").getValue(Long.class);
                    int  newQty = (qty != null ? qty.intValue() : 1) + 1;
                    Map<String, Object> u = new HashMap<>();
                    u.put("quantity",   newQty);
                    u.put("totalPrice", food.getPrice() * newQty);
                    itemRef.updateChildren(u);
                    Toast.makeText(RestaurantMenuActivity.this,
                            food.getName() + " qty updated 🛒", Toast.LENGTH_SHORT).show();
                } else {
                    Map<String, Object> cartData = new HashMap<>();
                    cartData.put("foodId",       foodId);
                    cartData.put("name",         food.getName());
                    cartData.put("price",        food.getPrice());
                    cartData.put("quantity",     1);
                    cartData.put("totalPrice",   food.getPrice());
                    cartData.put("restaurantId", restaurantId);
                    itemRef.setValue(cartData)
                            .addOnSuccessListener(u -> {
                                Toast.makeText(RestaurantMenuActivity.this,
                                        food.getName() + " added to cart 🛒",
                                        Toast.LENGTH_SHORT).show();
                                updateCartBar(); // refresh cart bar after adding
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(RestaurantMenuActivity.this,
                                            "Failed to add", Toast.LENGTH_SHORT).show());
                }
                updateCartBar();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }
}