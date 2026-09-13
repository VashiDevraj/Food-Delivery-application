package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.databinding.ActivityOrderHistoryBinding;
import com.example.fooddeliveryapp.models.CartItem;
import com.example.fooddeliveryapp.models.Order;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OrderHistoryActivity — user sees past orders.
 *
 * ✅ Review button visible only for DELIVERED + not yet reviewed orders.
 * ✅ Cancel button visible only for PENDING orders.
 * ✅ Order notes displayed if present.
 * ✅ Color-coded status badges including Cancelled & Out for Delivery.
 * ✅ All static-class references to outer activity go through constructor params (no more 'this' bugs).
 */
public class OrderHistoryActivity extends AppCompatActivity {

    private ActivityOrderHistoryBinding binding;
    private SessionManager              sessionManager;
    private DatabaseReference           dbRef;
    private final List<Order>           orderList = new ArrayList<>();
    private String                      userId;
    private OrderHistoryAdapter         adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding        = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();
        userId = sessionManager.getUid();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Orders");
        }

        // Pass dbRef and context callbacks via lambdas — no 'this' leaks into static class.
        adapter = new OrderHistoryAdapter(
                orderList,
                this::openReview,
                this::showCancelDialog);

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);

        loadOrders();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openReview(Order order) {
        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra("orderId",        order.getOrderId());
        intent.putExtra("restaurantId",   order.getRestaurantId());
        intent.putExtra("restaurantName", order.getRestaurantName());
        intent.putExtra("deliveryBoyId", order.getDeliveryBoyId());
        intent.putExtra("deliveryBoyName", order.getDeliveryBoyName());
        intent.putExtra("tipAmount", order.getTipAmount());
        startActivity(intent);
    }

    // ── Load orders ───────────────────────────────────────────────────────────

    private void loadOrders() {
        binding.progressBar.setVisibility(View.VISIBLE);

        dbRef.child(Constants.NODE_ORDERS)
                .orderByChild("userId")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        binding.progressBar.setVisibility(View.GONE);
                        orderList.clear();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Order order = snap.getValue(Order.class);
                            if (order != null) {
                                order.setOrderId(snap.getKey());
                                Boolean reviewed = snap.child("reviewed").getValue(Boolean.class);
                                if (reviewed != null && reviewed) order.setReviewed(true);
                                orderList.add(order);
                            }
                        }

                        Collections.sort(orderList,
                                (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                        if (orderList.isEmpty()) {
                            binding.tvNoOrders.setVisibility(View.VISIBLE);
                            binding.rvOrders.setVisibility(View.GONE);
                        } else {
                            binding.tvNoOrders.setVisibility(View.GONE);
                            binding.rvOrders.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(OrderHistoryActivity.this,
                                "Error loading orders: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Cancel flow (lives in the Activity so it has dbRef + context) ─────────

    /**
     * Shows the cancel confirmation dialog.
     * Called from the adapter via the OnCancelClick lambda.
     */
    private void showCancelDialog(Order order) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Reason for cancellation (optional)");

        new AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setView(input)
                .setPositiveButton("Yes, Cancel Order", (d, w) -> {
                    String reason = input.getText().toString().trim();
                    cancelOrder(order, reason);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelOrder(Order order, String reason) {
        String orderId = order.getOrderId();
        if (orderId == null || orderId.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status",      Constants.STATUS_CANCELLED);
        updates.put("cancelReason", reason);
        updates.put("cancelledBy",  "user");

        // Non-COD orders get a refund flag
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            updates.put("paymentStatus", "Refunded");
        }

        // ── Global Orders node ────────────────────────────────────────────────
        dbRef.child(Constants.NODE_ORDERS).child(orderId)
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    // ── User Orders copy ──────────────────────────────────────
                    String uid = order.getUserId();
                    if (uid != null && !uid.isEmpty()) {
                        dbRef.child(Constants.NODE_USERS)
                                .child(uid)
                                .child(Constants.NODE_MY_ORDERS)
                                .child(orderId)
                                .updateChildren(updates);
                    }
                    showRefundDialog(order);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Cancellation failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void showRefundDialog(Order order) {
        String msg;
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            msg = "Order cancelled.\nNo payment was made (COD).";
        } else {
            msg = "Order cancelled.\n₹"
                    + String.format(Locale.getDefault(), "%.2f", order.getTotalAmount())
                    + " will be refunded within 3–5 working days.";
        }
        new AlertDialog.Builder(this)
                .setTitle("Order Cancelled")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    // ── Back navigation ───────────────────────────────────────────────────────

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Adapter
    // ─────────────────────────────────────────────────────────────────────────

    /** Callback interfaces — passed as constructor params so the static adapter
     *  never needs to reference the outer activity via 'this'. */
    interface OnReviewClick { void onClick(Order order); }
    interface OnCancelClick { void onClick(Order order); }

    static class OrderHistoryAdapter
            extends RecyclerView.Adapter<OrderHistoryAdapter.VH> {

        private final List<Order>   orders;
        private final OnReviewClick reviewListener;
        private final OnCancelClick cancelListener;

        OrderHistoryAdapter(List<Order> orders,
                            OnReviewClick reviewListener,
                            OnCancelClick cancelListener) {
            this.orders         = orders;
            this.reviewListener = reviewListener;
            this.cancelListener = cancelListener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Order order  = orders.get(pos);
            String status = order.getStatus();

            // ── Order ID ──────────────────────────────────────────────────────
            h.tvOrderId.setText(order.getShortOrderId());

            // ── Timestamp ─────────────────────────────────────────────────────
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd MMM yyyy  •  hh:mm a", Locale.getDefault());
            h.tvOrderTime.setText(sdf.format(new Date(order.getTimestamp())));

            // ── Amount ────────────────────────────────────────────────────────
            h.tvOrderAmount.setText(
                    String.format(Locale.getDefault(), "₹%.2f", order.getTotalAmount()));

            // ── Items ─────────────────────────────────────────────────────────
            h.tvOrderItems.setText(buildItemsSummary(order));

            // ── Status badge ──────────────────────────────────────────────────
            h.tvOrderStatus.setText(getStatusLabel(status));
            applyStatusColor(h.tvOrderStatus, status);

            // ── Notes ─────────────────────────────────────────────────────────
            String notes = order.getNotes();
            if (notes != null && !notes.isEmpty()
                    && !"N/A".equals(notes) && !"null".equals(notes)) {
                h.tvOrderNotes.setVisibility(View.VISIBLE);
                h.tvOrderNotes.setText("📝 Note: " + notes);
            } else {
                h.tvOrderNotes.setVisibility(View.GONE);
            }

            // ── Progress tracker ──────────────────────────────────────────────
            boolean isPreparing       = Constants.STATUS_PREPARING.equals(status);
            boolean isOutForDelivery  = Constants.STATUS_OUT_FOR_DELIVERY.equals(status);
            boolean isDelivered       = Constants.STATUS_DELIVERED.equals(status);
            boolean isCancelled       = Constants.STATUS_CANCELLED.equals(status);

            // Step 1 (Placed) — always active
            h.stepPlaced.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFE23744));

            // Step 2 (Preparing)
            boolean step2 = isPreparing || isOutForDelivery || isDelivered;
            h.stepPreparing.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(step2 ? 0xFFE23744 : 0xFFBDBDBD));
            h.lineOneTwoConnector.setBackgroundColor(step2 ? 0xFFE23744 : 0xFFBDBDBD);

            // Step 3 (Delivered)
            h.stepDelivered.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            isDelivered ? 0xFF4CAF50 : 0xFFBDBDBD));
            h.lineTwoThreeConnector.setBackgroundColor(
                    isDelivered ? 0xFF4CAF50 : 0xFFBDBDBD);

            // ── Cancel button — only for Pending orders ───────────────────────
            if (Constants.STATUS_PENDING.equals(status)) {
                h.btnCancelOrder.setVisibility(View.VISIBLE);
                h.btnCancelOrder.setOnClickListener(v -> cancelListener.onClick(order));
            } else {
                h.btnCancelOrder.setVisibility(View.GONE);
            }

            // ── Review button — only for Delivered orders ─────────────────────
            if (isDelivered) {
                h.btnWriteReview.setVisibility(View.VISIBLE);
                if (order.isReviewed()) {
                    h.btnWriteReview.setText("Reviewed ✓");
                    h.btnWriteReview.setEnabled(false);
                    h.btnWriteReview.setAlpha(0.6f);
                } else {
                    h.btnWriteReview.setText("⭐  Rate this order");
                    h.btnWriteReview.setEnabled(true);
                    h.btnWriteReview.setAlpha(1f);
                    h.btnWriteReview.setOnClickListener(v -> reviewListener.onClick(order));
                }
            } else {
                h.btnWriteReview.setVisibility(View.GONE);
            }
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), OrderTrackingActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                v.getContext().startActivity(intent);
            });
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private static String getStatusLabel(String status) {
            if (status == null) return "⏳ Pending";
            switch (status) {
                case Constants.STATUS_PREPARING:        return "🍳 Preparing";
                case Constants.STATUS_OUT_FOR_DELIVERY: return "🚗 Out for Delivery";
                case Constants.STATUS_DELIVERED:        return "✅ Delivered";
                case Constants.STATUS_CANCELLED:        return "❌ Cancelled";
                default:                                return "⏳ Pending";
            }
        }

        private static void applyStatusColor(TextView tv, String status) {
            if (status == null || Constants.STATUS_PENDING.equals(status)) {
                tv.setBackgroundResource(R.drawable.bg_status_pending);
            } else if (Constants.STATUS_PREPARING.equals(status)
                    || Constants.STATUS_OUT_FOR_DELIVERY.equals(status)) {
                tv.setBackgroundResource(R.drawable.bg_status_preparing);
            } else if (Constants.STATUS_DELIVERED.equals(status)) {
                tv.setBackgroundResource(R.drawable.bg_status_delivered);
            } else if (Constants.STATUS_CANCELLED.equals(status)) {
                tv.setBackgroundResource(R.drawable.bg_status_cancelled);
            } else {
                tv.setBackgroundResource(R.drawable.bg_status_pending);
            }
        }

        private static String buildItemsSummary(Order order) {
            if (order.getItems() == null || order.getItems().isEmpty()) return "No items";
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, CartItem> entry : order.getItems().entrySet()) {
                CartItem item = entry.getValue();
                if (item != null) {
                    sb.append("• ")
                            .append(item.getName() != null ? item.getName() : "Item")
                            .append("  ×").append(item.getQuantity())
                            .append("  —  ₹")
                            .append(String.format(Locale.getDefault(), "%.0f",
                                    item.getPrice() * item.getQuantity()))
                            .append("\n");
                }
            }
            return sb.toString().trim();
        }

        @Override
        public int getItemCount() { return orders.size(); }

        // ── ViewHolder ────────────────────────────────────────────────────────

        static class VH extends RecyclerView.ViewHolder {
            TextView       tvOrderId, tvOrderStatus, tvOrderAmount,
                    tvOrderTime, tvOrderItems, tvOrderNotes;
            View           stepPlaced, stepPreparing, stepDelivered;
            View           lineOneTwoConnector, lineTwoThreeConnector;
            MaterialButton btnWriteReview;
            MaterialButton btnCancelOrder;   // ← correctly scoped inside VH

            VH(@NonNull View v) {
                super(v);
                tvOrderId             = v.findViewById(R.id.tvOrderId);
                tvOrderStatus         = v.findViewById(R.id.tvOrderStatus);
                tvOrderAmount         = v.findViewById(R.id.tvOrderAmount);
                tvOrderTime           = v.findViewById(R.id.tvOrderTime);
                tvOrderItems          = v.findViewById(R.id.tvOrderItems);
                tvOrderNotes          = v.findViewById(R.id.tvOrderNotes);
                stepPlaced            = v.findViewById(R.id.stepPlaced);
                stepPreparing         = v.findViewById(R.id.stepPreparing);
                stepDelivered         = v.findViewById(R.id.stepDelivered);
                lineOneTwoConnector   = v.findViewById(R.id.lineOneTwoConnector);
                lineTwoThreeConnector = v.findViewById(R.id.lineTwoThreeConnector);
                btnWriteReview        = v.findViewById(R.id.btnWriteReview);
                btnCancelOrder        = v.findViewById(R.id.btnCancelOrder);
            }
        }
    }
}