package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.adapters.CartAdapter;
import com.example.fooddeliveryapp.databinding.ActivityCartBinding;
import com.example.fooddeliveryapp.models.CartItem;
import com.example.fooddeliveryapp.models.FoodItem;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private ActivityCartBinding  binding;
    private SessionManager       sessionManager;
    private DatabaseReference    databaseReference;
    private CartAdapter          cartAdapter;

    private List<CartItem> cartItemList     = new ArrayList<>();
    private String         userId;
    private String         cartRestaurantId = null;

    private double  currentGrandTotal  = 0.0;

    // ── Coupon slots ──────────────────────────────────────────────────────────
    private double  appliedDiscount1   = 0.0;
    private String  appliedCouponCode1 = "";
    private double  appliedDiscount2   = 0.0;
    private String  appliedCouponCode2 = "";
    /** true when FREEDEL occupies slot 2 — delivery fee is then zeroed */
    private boolean slot2IsDelivery    = false;

    private static final double DELIVERY_FEE  = 40.0;
    private static final double PLATFORM_FEE  = 5.0;
    private static final int    REQUEST_COUPON = 101;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager    = new SessionManager(this);
        databaseReference = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();
        userId = sessionManager.getUid();

        setupToolbar();
        setupRecyclerView();
        loadCartItems();

        binding.btnApplyCoupon.setOnClickListener(v -> {
            if (!appliedCouponCode1.isEmpty() && !appliedCouponCode2.isEmpty()) {
                Toast.makeText(this,
                        "You can only apply 2 coupons at a time.\nRemove one to add another.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(this, CouponActivity.class);
            i.putExtra("cartSubtotal", computeSubtotal());        // BUG FIX: real subtotal
            i.putExtra("fillingSlot",  appliedCouponCode1.isEmpty() ? 1 : 2);
            i.putExtra("appliedCode1", appliedCouponCode1);
            i.putExtra("appliedCode2", appliedCouponCode2);
            startActivityForResult(i, REQUEST_COUPON);
        });

        binding.btnPlaceOrder.setOnClickListener(v -> checkRestaurantBeforeOrder());
        binding.btnAddMoreItems.setOnClickListener(v -> finish());
    }

    // ── Coupon result ─────────────────────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_COUPON || resultCode != RESULT_OK || data == null) return;

        String  code      = data.getStringExtra("couponCode");
        double  discount  = data.getDoubleExtra("discount", 0);
        boolean isFreeDel = data.getBooleanExtra("isFreeDelivery", false);
        int     slot      = data.getIntExtra("slot", 1);

        if (isFreeDel) {
            appliedCouponCode2 = code;
            appliedDiscount2   = 0;    // monetary discount = 0; delivery fee is zeroed separately
            slot2IsDelivery    = true;
        } else if (slot == 1) {
            appliedCouponCode1 = code;
            appliedDiscount1   = discount;
        } else {
            appliedCouponCode2 = code;
            appliedDiscount2   = discount;
            slot2IsDelivery    = false;
        }

        updateCouponChipsUI();
        updateTotalUI();
    }

    // ── Coupon chip label ─────────────────────────────────────────────────────
    private void updateCouponChipsUI() {
        StringBuilder label = new StringBuilder();
        if (!appliedCouponCode1.isEmpty()) label.append(appliedCouponCode1);
        if (!appliedCouponCode2.isEmpty()) {
            if (label.length() > 0) label.append("  +  ");
            label.append(appliedCouponCode2);
        }

        if (label.length() > 0) {
            binding.tvCouponApplied.setVisibility(View.VISIBLE);
            binding.tvApplyCoupon.setText("Coupons: " + label);
            binding.layoutCouponDiscount.setVisibility(View.VISIBLE);
        } else {
            binding.tvCouponApplied.setVisibility(View.GONE);
            binding.tvApplyCoupon.setText("Apply coupon");
            binding.layoutCouponDiscount.setVisibility(View.GONE);
        }
    }

    // ── Subtotal helper ───────────────────────────────────────────────────────
    private double computeSubtotal() {
        double s = 0;
        for (CartItem item : cartItemList) s += item.getTotalPrice();
        return s;
    }

    // ── Bill summary UI ───────────────────────────────────────────────────────
    private void updateTotalUI() {
        double subtotal  = 0;
        int    itemCount = 0;
        for (CartItem item : cartItemList) {
            subtotal  += item.getTotalPrice();
            itemCount += item.getQuantity();
        }

        double deliveryFee    = slot2IsDelivery ? 0.0 : DELIVERY_FEE;
        double discountAmount = appliedDiscount1 + appliedDiscount2;
        double taxable        = Math.max(0, subtotal - discountAmount);
        double tax            = taxable * 0.05;
        double grand          = taxable + deliveryFee + PLATFORM_FEE + tax;
        if (grand < 0) grand = 0;
        currentGrandTotal = grand;

        // Basic rows
        binding.tvItemCount.setText(itemCount + " items");
        binding.tvTotalAmount.setText("₹" + String.format("%.2f", subtotal));
        binding.tvTaxes.setText("₹" + String.format("%.2f", tax));
        binding.tvGrandTotal.setText("₹" + String.format("%.2f", grand));

        // Delivery fee label
        if (slot2IsDelivery) {
            binding.tvDeliveryFee.setText("Free 🎉");
            binding.tvDeliveryFee.setTextColor(0xFF388E3C);
        } else {
            binding.tvDeliveryFee.setText("₹" + String.format("%.2f", DELIVERY_FEE));
            binding.tvDeliveryFee.setTextColor(0xFF212121);
        }

        // ── Coupon discount row 1 ─────────────────────────────────────────
        if (appliedDiscount1 > 0) {
            binding.tvCouponDiscount.setText("-₹" + String.format("%.0f", appliedDiscount1));
            binding.layoutCouponDiscount.setVisibility(View.VISIBLE);
        } else {
            binding.layoutCouponDiscount.setVisibility(View.GONE);
        }

        // ── Free delivery discount row (FREEDEL) ──────────────────────────
        // VIEW EXISTS in activity_cart.xml → direct binding access, no try/catch
        if (slot2IsDelivery) {
            binding.tvDeliveryDiscount.setText("-₹" + String.format("%.0f", DELIVERY_FEE));
            binding.layoutDeliveryDiscount.setVisibility(View.VISIBLE);
        } else {
            binding.layoutDeliveryDiscount.setVisibility(View.GONE);
        }

        // ── Coupon discount row 2 (second discount coupon) ────────────────
        // VIEW EXISTS in activity_cart.xml → direct binding access, no try/catch
        if (!slot2IsDelivery && appliedDiscount2 > 0) {
            binding.tvCouponDiscount2.setText("-₹" + String.format("%.0f", appliedDiscount2));
            binding.layoutCouponDiscount2.setVisibility(View.VISIBLE);
        } else {
            binding.layoutCouponDiscount2.setVisibility(View.GONE);
        }

        // ── Savings banner ────────────────────────────────────────────────
        double totalSaved = discountAmount + (slot2IsDelivery ? DELIVERY_FEE : 0);
        if (totalSaved > 0) {
            binding.tvSavings.setVisibility(View.VISIBLE);
            binding.tvSavings.setText(
                    "🎉 You're saving ₹" + String.format("%.0f", totalSaved) + " on this order!");
        } else {
            binding.tvSavings.setVisibility(View.GONE);
        }
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Cart");
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────
    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, cartItemList, new CartAdapter.CartActionListener() {
            @Override
            public void onIncreaseQuantity(CartItem cartItem, int position) {
                int    newQty   = cartItem.getQuantity() + 1;
                double newTotal = cartItem.getPrice() * newQty;
                updateCartItemQuantity(cartItem.getFoodId(), newQty, newTotal);
            }

            @Override
            public void onDecreaseQuantity(CartItem cartItem, int position) {
                if (cartItem.getQuantity() > 1) {
                    int    newQty   = cartItem.getQuantity() - 1;
                    double newTotal = cartItem.getPrice() * newQty;
                    updateCartItemQuantity(cartItem.getFoodId(), newQty, newTotal);
                } else {
                    removeCartItem(cartItem.getFoodId());
                }
            }

            @Override
            public void onRemoveItem(CartItem cartItem, int position) {
                new AlertDialog.Builder(CartActivity.this)
                        .setTitle("Remove Item")
                        .setMessage("Remove " + cartItem.getName() + " from cart?")
                        .setPositiveButton("Remove", (dialog, which) ->
                                removeCartItem(cartItem.getFoodId()))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCartItems.setAdapter(cartAdapter);
    }

    // ── Firebase: load cart ───────────────────────────────────────────────────
    private void loadCartItems() {
        databaseReference
                .child(Constants.NODE_USERS).child(userId).child(Constants.NODE_CART)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        cartItemList.clear();
                        cartRestaurantId = null;

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            CartItem item = snap.getValue(CartItem.class);
                            if (item != null) {
                                item.setFoodId(snap.getKey());
                                cartItemList.add(item);
                                if (cartRestaurantId == null) {
                                    String rid = snap.child("restaurantId").getValue(String.class);
                                    if (rid != null) cartRestaurantId = rid;
                                }
                            }
                        }

                        if (cartItemList.isEmpty()) {
                            showEmptyCart();
                        } else {
                            binding.layoutEmptyCart.setVisibility(View.GONE);
                            binding.rvCartItems.setVisibility(View.VISIBLE);
                            binding.layoutTotal.setVisibility(View.VISIBLE);
                            loadRestaurantInfo();
                            loadSuggestedItems();
                        }

                        cartAdapter.notifyDataSetChanged();
                        updateTotalUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CartActivity.this,
                                "Failed to load cart", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadRestaurantInfo() {
        if (cartRestaurantId == null) return;
        databaseReference
                .child(Constants.NODE_ADMINS).child(cartRestaurantId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_RESTAURANT_INFO)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        String city = snapshot.child("city").getValue(String.class);
                        String time = snapshot.child("deliveryTime").getValue(String.class);
                        if (name != null) binding.tvCartRestaurantName.setText(name);
                        if (time != null) binding.tvCartDeliveryTime.setText(time);
                        else if (city != null) binding.tvCartDeliveryTime.setText(city);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadSuggestedItems() {
        if (cartRestaurantId == null) return;
        List<String> cartFoodIds = new ArrayList<>();
        for (CartItem ci : cartItemList) cartFoodIds.add(ci.getFoodId());

        databaseReference
                .child(Constants.NODE_ADMINS).child(cartRestaurantId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_MENU)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        binding.llSuggestedItems.removeAllViews();
                        int added = 0;
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            if (added >= 8) break;
                            FoodItem item = snap.getValue(FoodItem.class);
                            if (item == null || !item.isAvailable()) continue;
                            if (cartFoodIds.contains(item.getFoodId())) continue;

                            View card = LayoutInflater.from(CartActivity.this)
                                    .inflate(R.layout.item_suggested_food, null);

                            ImageView iv     = card.findViewById(R.id.ivSuggestedImage);
                            TextView  tvName  = card.findViewById(R.id.tvSuggestedName);
                            TextView  tvPrice = card.findViewById(R.id.tvSuggestedPrice);
                            View      btnAdd  = card.findViewById(R.id.btnAddSuggested);

                            tvName.setText(item.getName());
                            tvPrice.setText("₹" + String.format("%.0f", item.getPrice()));

                            if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
                                try {
                                    byte[] bytes = Base64.decode(item.getImageBase64(), Base64.DEFAULT);
                                    Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    if (bmp != null) iv.setImageBitmap(bmp);
                                } catch (Exception ignored) {}
                            }

                            FoodItem finalItem = item;
                            btnAdd.setOnClickListener(v -> addSuggestedToCart(finalItem));

                            int dp120 = (int)(getResources().getDisplayMetrics().density * 120);
                            int dp8   = (int)(getResources().getDisplayMetrics().density * 8);
                            LinearLayout.LayoutParams lp =
                                    new LinearLayout.LayoutParams(dp120,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp.setMarginEnd(dp8);
                            card.setLayoutParams(lp);
                            binding.llSuggestedItems.addView(card);
                            added++;
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void addSuggestedToCart(FoodItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("foodId",       item.getFoodId());
        data.put("name",         item.getName());
        data.put("price",        item.getPrice());
        data.put("quantity",     1);
        data.put("totalPrice",   item.getPrice());
        data.put("restaurantId", item.getRestaurantId());
        databaseReference
                .child(Constants.NODE_USERS).child(userId).child(Constants.NODE_CART)
                .child(item.getFoodId())
                .setValue(data)
                .addOnSuccessListener(u ->
                        Toast.makeText(this, item.getName() + " added ✓", Toast.LENGTH_SHORT).show());
    }

    private void showEmptyCart() {
        binding.layoutEmptyCart.setVisibility(View.VISIBLE);
        binding.rvCartItems.setVisibility(View.GONE);
        binding.layoutTotal.setVisibility(View.GONE);
        currentGrandTotal = 0.0;
    }

    private void updateCartItemQuantity(String foodId, int newQty, double newTotal) {
        Map<String, Object> map = new HashMap<>();
        map.put("quantity",   newQty);
        map.put("totalPrice", newTotal);
        databaseReference
                .child(Constants.NODE_USERS).child(userId).child(Constants.NODE_CART).child(foodId)
                .updateChildren(map);
    }

    private void removeCartItem(String foodId) {
        databaseReference
                .child(Constants.NODE_USERS).child(userId).child(Constants.NODE_CART).child(foodId)
                .removeValue();
    }

    private void showOrderConfirmationDialog() {
        if (cartItemList.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Confirm Order")
                .setMessage("Proceed to checkout?\nTotal: ₹" + String.format("%.2f", currentGrandTotal))
                .setPositiveButton("Proceed", (d, w) -> placeOrder())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void placeOrder() {
        String notes = binding.etOrderNotes.getText() != null
                ? binding.etOrderNotes.getText().toString().trim() : "";

        double totalDiscount = appliedDiscount1 + appliedDiscount2
                + (slot2IsDelivery ? DELIVERY_FEE : 0);

        StringBuilder codes = new StringBuilder();
        if (!appliedCouponCode1.isEmpty()) codes.append(appliedCouponCode1);
        if (!appliedCouponCode2.isEmpty()) {
            if (codes.length() > 0) codes.append(", ");
            codes.append(appliedCouponCode2);
        }

        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("totalAmount",  currentGrandTotal);
        intent.putExtra("notes",        notes);
        intent.putExtra("couponCode",   codes.toString());
        intent.putExtra("discount",     totalDiscount);
        intent.putExtra("restaurantId", cartRestaurantId != null ? cartRestaurantId : "");
        startActivity(intent);
    }
    private void checkRestaurantBeforeOrder() {

        if (cartRestaurantId == null) {
            Toast.makeText(this, "Invalid restaurant", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference
                .child(Constants.NODE_RESTAURANTS)
                .child(cartRestaurantId)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            Toast.makeText(CartActivity.this,
                                    "Restaurant not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        com.example.fooddeliveryapp.models.Restaurant r =
                                snapshot.getValue(com.example.fooddeliveryapp.models.Restaurant.class);

                        if (r != null) {

                            Boolean openVal   = snapshot.child("isOpen").getValue(Boolean.class);
                            Boolean manualVal = snapshot.child("manualOverride").getValue(Boolean.class);
                            String openT      = snapshot.child("openTime").getValue(String.class);
                            String closeT     = snapshot.child("closeTime").getValue(String.class);

                            if (openVal != null) r.setOpen(openVal);
                            if (manualVal != null) r.setManualOverride(manualVal);
                            if (openT != null) r.setOpenTime(openT);
                            if (closeT != null) r.setCloseTime(closeT);

                            if (!com.example.fooddeliveryapp.utils.RestaurantStatusHelper.isOpen(r)) {

                                new AlertDialog.Builder(CartActivity.this)
                                        .setTitle("🔴 Restaurant Closed")
                                        .setMessage("This restaurant is currently closed.\n\nTry again later.")
                                        .setPositiveButton("OK", null)
                                        .show();

                                return;
                            }

                            showOrderConfirmationDialog();

                        } else {
                            Toast.makeText(CartActivity.this,
                                    "Error reading restaurant data", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CartActivity.this,
                                "Failed to check restaurant status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}