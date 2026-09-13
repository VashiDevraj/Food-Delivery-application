package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fooddeliveryapp.adapters.AdminOrderAdapter;
import com.example.fooddeliveryapp.databinding.ActivityAdminOrdersBinding;
import com.example.fooddeliveryapp.models.Order;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminOrdersActivity extends AppCompatActivity {

    private ActivityAdminOrdersBinding binding;
    private DatabaseReference          dbRef;
    private AdminOrderAdapter          adminOrderAdapter;
    private final List<Order>          orderList = new ArrayList<>();
    private ValueEventListener         ordersListener;
    private final ExecutorService      executor  = Executors.newSingleThreadExecutor();

    // ── FIX: add adminId field ────────────────────────────────────────────────
    private String adminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ── FIX: get adminId from session ─────────────────────────────────────
        adminId = new SessionManager(this).getUid();

        dbRef = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Orders");
        }

        setupRecyclerView();
        loadAllOrders();
    }

    private void setupRecyclerView() {
        adminOrderAdapter = new AdminOrderAdapter(
                this, orderList,
                this::updateOrderStatus,
                this::openAssignDeliveryBoyScreen
        );
        binding.rvAdminOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAdminOrders.setAdapter(adminOrderAdapter);
        binding.rvAdminOrders.setHasFixedSize(false);
    }

    private void loadAllOrders() {
        showLoading(true);
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);
                orderList.clear();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    showEmptyState(true);
                    adminOrderAdapter.notifyDataSetChanged();
                    return;
                }

                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        // ── FIX: filter orders by this admin's restaurantId ───
                        String rid = snap.child("restaurantId").getValue(String.class);
                        if (!adminId.equals(rid)) continue;

                        Order order = snap.getValue(Order.class);
                        if (order != null) {
                            order.setOrderId(snap.getKey());
                            orderList.add(order);
                        }
                    } catch (Exception ignored) {}
                }

                Collections.sort(orderList,
                        (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                showEmptyState(orderList.isEmpty());
                if (!orderList.isEmpty())
                    binding.tvOrderCount.setText("Total Orders: " + orderList.size());
                adminOrderAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(AdminOrdersActivity.this,
                        "Failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        dbRef.child(Constants.NODE_ORDERS).addValueEventListener(ordersListener);
    }

    private void openAssignDeliveryBoyScreen(Order order) {
        if ("REJECTED".equals(order.getDeliveryStatus())) {
            Toast.makeText(this, "⚠️ Previous delivery boy rejected. Reassign required", Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(this, AssignDeliveryBoyActivity.class);
        intent.putExtra(AssignDeliveryBoyActivity.EXTRA_ORDER_ID,        order.getOrderId());
        intent.putExtra(AssignDeliveryBoyActivity.EXTRA_ORDER_AMOUNT,    order.getTotalAmount());
        intent.putExtra(AssignDeliveryBoyActivity.EXTRA_ORDER_SHORT_ID,  order.getShortOrderId());
        intent.putExtra(AssignDeliveryBoyActivity.EXTRA_RESTAURANT_NAME, order.getRestaurantName());
        intent.putExtra(AssignDeliveryBoyActivity.EXTRA_USER_ID,         order.getUserId());
        intent.putExtra(AssignDeliveryBoyActivity.EXTRA_TIP_AMOUNT,      order.getTipAmount());
        intent.putExtra(AssignDeliveryBoyActivity.EXTRA_CURRENT_BOY_ID,  order.getDeliveryBoyId());
        startActivity(intent);
    }

    private void updateOrderStatus(Order order, String newStatus) {
        String orderId = order.getOrderId();
        if (orderId == null || orderId.isEmpty()) return;

        boolean isCOD          = "COD".equalsIgnoreCase(order.getPaymentMethod());
        boolean isNowDelivered = Constants.STATUS_DELIVERED.equals(newStatus);
        boolean isNowCancelled = Constants.STATUS_CANCELLED.equals(newStatus);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        if (isNowCancelled) {
            updates.put("cancelReason", order.getCancelReason() != null ? order.getCancelReason() : "");
            updates.put("cancelledBy",  "admin");
            if (!isCOD) updates.put("paymentStatus", "Refunded");
        }

        if (isCOD && isNowDelivered) updates.put("paymentStatus", "Paid");

        dbRef.child(Constants.NODE_ORDERS).child(orderId).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    if (order.getUserId() != null) {
                        dbRef.child(Constants.NODE_USERS).child(order.getUserId())
                                .child(Constants.NODE_MY_ORDERS).child(orderId).updateChildren(updates);
                    }
                    if (!order.getDeliveryBoyId().isEmpty()) {
                        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(order.getDeliveryBoyId())
                                .child(Constants.NODE_ASSIGNED_ORDERS).child(orderId).updateChildren(updates);

                        if (isNowDelivered) {
                            recordDeliveryBoyEarnings(order);
                        }
                    }
                    Toast.makeText(this, "Status → " + newStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void recordDeliveryBoyEarnings(Order order) {
        String dbUid = order.getDeliveryBoyId();
        if (dbUid == null || dbUid.isEmpty()) return;

        double commission   = Constants.BASE_DELIVERY_FEE +
                (order.getTotalAmount() * Constants.DELIVERY_COMMISSION_PERCENT / 100.0);
        double totalEarning = commission + order.getTipAmount();

        String earningId = dbRef.child(Constants.NODE_DELIVERY_BOYS).child(dbUid)
                .child(Constants.NODE_EARNING_HISTORY).push().getKey();
        if (earningId == null) return;

        Map<String, Object> earning = new HashMap<>();
        earning.put("earningId",      earningId);
        earning.put("orderId",        order.getOrderId());
        earning.put("restaurantName", order.getRestaurantName());
        earning.put("customerName",   order.getUserName());
        earning.put("orderAmount",    order.getTotalAmount());
        earning.put("tipAmount",      order.getTipAmount());
        earning.put("commission",     commission);
        earning.put("totalEarning",   totalEarning);
        earning.put("timestamp",      System.currentTimeMillis());

        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(dbUid)
                .child(Constants.NODE_EARNING_HISTORY).child(earningId).setValue(earning);

        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(dbUid)
                .child(Constants.NODE_PROFILE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        Integer deliveries = snap.child("totalDeliveries").getValue(Integer.class);
                        Double  totalEarn  = snap.child("totalEarnings").getValue(Double.class);
                        Double  monthEarn  = snap.child("monthEarnings").getValue(Double.class);

                        int    newDeliveries = (deliveries != null ? deliveries : 0) + 1;
                        double newTotal      = (totalEarn  != null ? totalEarn  : 0) + totalEarning;
                        double newMonth      = (monthEarn  != null ? monthEarn  : 0) + totalEarning;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("totalDeliveries", newDeliveries);
                        updates.put("totalEarnings",   newTotal);
                        updates.put("monthEarnings",   newMonth);

                        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(dbUid)
                                .child(Constants.NODE_PROFILE).updateChildren(updates);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            binding.rvAdminOrders.setVisibility(View.GONE);
            binding.tvNoOrders.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean empty) {
        binding.tvNoOrders.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvAdminOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        if (ordersListener != null)
            dbRef.child(Constants.NODE_ORDERS).removeEventListener(ordersListener);
    }
}