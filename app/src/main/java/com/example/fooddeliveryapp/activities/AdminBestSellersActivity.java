package com.example.fooddeliveryapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.databinding.ActivityAdminBestSellersBinding;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminBestSellersActivity extends AppCompatActivity {

    private static final String TAG = "BestSellers";

    private ActivityAdminBestSellersBinding binding;
    private DatabaseReference dbRef;
    private String adminId;

    /** foodId → tallied sales data */
    private final Map<String, BestSellerItem> tally = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBestSellersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adminId = new SessionManager(this).getUid();
        dbRef   = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Best Sellers 🏆");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadBestSellers();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 1 — tally sales from the orders node
    // ─────────────────────────────────────────────────────────────────────────
    private void loadBestSellers() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        dbRef.child(Constants.NODE_ORDERS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        tally.clear();
                        Log.d(TAG, "Total orders: " + snapshot.getChildrenCount());

                        for (DataSnapshot order : snapshot.getChildren()) {

                            // Filter by this restaurant
                            String rid = order.child("restaurantId").getValue(String.class);
                            if (rid == null) rid = order.child("restaurant_id").getValue(String.class);
                            if (!adminId.equals(rid)) continue;

                            // Tally every item in the order
                            for (DataSnapshot itemSnap : order.child("items").getChildren()) {
                                String foodId = itemSnap.child("foodId").getValue(String.class);
                                String name   = itemSnap.child("name").getValue(String.class);
                                Long   qty    = itemSnap.child("quantity").getValue(Long.class);
                                Double total  = itemSnap.child("totalPrice").getValue(Double.class);

                                if (total == null) {
                                    Double unit = itemSnap.child("price").getValue(Double.class);
                                    if (unit != null && qty != null) total = unit * qty;
                                }

                                if (foodId == null || name == null) continue;
                                long   q = qty   != null ? qty   : 1L;
                                double t = total != null ? total : 0.0;

                                // ── IMAGE FIX ─────────────────────────────────
                                // Orders do NOT store imageBase64 — only the menu
                                // node does. Leave imageBase64 empty here; we'll
                                // populate it in Step 2 from the menu node.
                                BestSellerItem existing = tally.get(foodId);
                                if (existing == null) {
                                    tally.put(foodId,
                                            new BestSellerItem(foodId, name, q, t, ""));
                                } else {
                                    existing.totalQty     += q;
                                    existing.totalRevenue += t;
                                }
                            }
                        }

                        if (tally.isEmpty()) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.tvEmpty.setVisibility(View.VISIBLE);
                            Log.d(TAG, "No items after filtering by restaurantId=" + adminId);
                        } else {
                            // Step 2 — enrich with images from the menu node
                            fetchImagesFromMenu();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "DB error (orders): " + error.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 2 — read the restaurant's menu to get imageBase64 for each foodId
    //
    // Root cause of missing images:
    //   Order items are saved with only: foodId, name, price, quantity,
    //   totalPrice, restaurantId.  imageBase64 is NEVER written to the order.
    //   So itemSnap.child("imageBase64") always returns null.
    //
    // Fix: fetch Admins/{adminId}/restaurant/menu once, then match by foodId.
    // ─────────────────────────────────────────────────────────────────────────
    private void fetchImagesFromMenu() {
        dbRef.child(Constants.NODE_ADMINS)
                .child(adminId)
                .child(Constants.NODE_RESTAURANT)
                .child(Constants.NODE_MENU)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot menuSnapshot) {

                        // Build a quick foodId → imageBase64 lookup from the menu
                        Map<String, String> imageMap = new HashMap<>();
                        for (DataSnapshot foodSnap : menuSnapshot.getChildren()) {
                            String fid = foodSnap.getKey();
                            // imageBase64 may be stored directly on the food node
                            String img = foodSnap.child("imageBase64").getValue(String.class);
                            if (img == null) img = foodSnap.child("image").getValue(String.class);
                            if (fid != null && img != null && !img.isEmpty()) {
                                imageMap.put(fid, img);
                            }
                        }

                        Log.d(TAG, "Menu images found: " + imageMap.size());

                        // Inject images into tallied items
                        for (BestSellerItem item : tally.values()) {
                            String img = imageMap.get(item.foodId);
                            if (img != null) item.imageBase64 = img;
                        }

                        // Sort and show top 10
                        List<BestSellerItem> sorted = new ArrayList<>(tally.values());
                        Collections.sort(sorted, (a, b) -> Long.compare(b.totalQty, a.totalQty));
                        List<BestSellerItem> top10 =
                                sorted.subList(0, Math.min(10, sorted.size()));

                        binding.progressBar.setVisibility(View.GONE);
                        if (top10.isEmpty()) {
                            binding.tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            binding.tvEmpty.setVisibility(View.GONE);
                            setupRecyclerView(top10);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Images couldn't be loaded — still show the list without images
                        Log.e(TAG, "DB error (menu): " + error.getMessage());
                        List<BestSellerItem> sorted = new ArrayList<>(tally.values());
                        Collections.sort(sorted, (a, b) -> Long.compare(b.totalQty, a.totalQty));
                        List<BestSellerItem> top10 =
                                sorted.subList(0, Math.min(10, sorted.size()));
                        binding.progressBar.setVisibility(View.GONE);
                        setupRecyclerView(top10);
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RecyclerView
    // ─────────────────────────────────────────────────────────────────────────
    private void setupRecyclerView(List<BestSellerItem> items) {
        binding.rvBestSellers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBestSellers.setAdapter(new RecyclerView.Adapter<BsViewHolder>() {

            @NonNull
            @Override
            public BsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_best_seller, parent, false);
                return new BsViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull BsViewHolder h, int position) {
                BestSellerItem item = items.get(position);
                int rank = position + 1;

                // Medal emoji for top 3, number badge for the rest
                h.tvRank.setText(rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉"
                        : "#" + rank);

                h.tvName.setText(item.name);
                h.tvQty.setText(item.totalQty + " sold");
                h.tvRevenue.setText("₹" + String.format("%.0f", item.totalRevenue));

                // Decode and display image (now populated from the menu node)
                if (item.imageBase64 != null && !item.imageBase64.isEmpty()) {
                    try {
                        byte[] bytes = Base64.decode(item.imageBase64, Base64.DEFAULT);
                        Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bmp != null) {
                            h.ivFood.setImageBitmap(bmp);
                        } else {
                            h.ivFood.setImageResource(R.drawable.ic_food_placeholder);
                        }
                    } catch (Exception e) {
                        h.ivFood.setImageResource(R.drawable.ic_food_placeholder);
                    }
                } else {
                    h.ivFood.setImageResource(R.drawable.ic_food_placeholder);
                }
            }

            @Override
            public int getItemCount() { return items.size(); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data model
    // ─────────────────────────────────────────────────────────────────────────
    static class BestSellerItem {
        String foodId, name, imageBase64;
        long   totalQty;
        double totalRevenue;

        BestSellerItem(String foodId, String name,
                       long totalQty, double totalRevenue, String imageBase64) {
            this.foodId       = foodId;
            this.name         = name;
            this.totalQty     = totalQty;
            this.totalRevenue = totalRevenue;
            this.imageBase64  = imageBase64;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────────────────────────────────────
    static class BsViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFood;
        TextView  tvRank, tvName, tvQty, tvRevenue;

        BsViewHolder(@NonNull View v) {
            super(v);
            ivFood    = v.findViewById(R.id.ivFoodImage);
            tvRank    = v.findViewById(R.id.tvRank);
            tvName    = v.findViewById(R.id.tvFoodName);
            tvQty     = v.findViewById(R.id.tvQtySold);
            tvRevenue = v.findViewById(R.id.tvRevenue);
        }
    }
}