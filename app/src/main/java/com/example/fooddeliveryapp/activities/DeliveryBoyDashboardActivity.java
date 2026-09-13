package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.Order;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeliveryBoyDashboardActivity — Maps and GPS entirely removed.
 *
 * KEY BEHAVIOURS:
 *  • When a new order is assigned (deliveryStatus=PENDING): Accept/Reject buttons + 60-second timer shown.
 *  • When the delivery boy taps Accept: timer and buttons disappear immediately, status badge shown.
 *  • When the delivery boy taps Reject or timer expires: order is unassigned.
 *  • Tapping an accepted order row opens DeliveryOrderDetailActivity.
 *  • Online/Offline toggle works via Firebase only (no GPS).
 */
public class DeliveryBoyDashboardActivity extends AppCompatActivity {

    private SessionManager    sessionManager;
    private DatabaseReference dbRef;

    private Chip     chipOnlineToggle;
    private TextView tvDbName, tvDbRating, tvDbTodayEarnings,
            tvDbTotalDeliveries, tvDbTotalEarnings, tvDbMonthEarnings,
            tvActiveOrdersEmpty;
    private RecyclerView     rvActiveOrders;
    private MaterialCardView cardEarnings, cardProfile, cardHistory, cardRatings;

    private final List<Order>                 activeOrders = new ArrayList<>();
    private final Map<String, CountDownTimer> timerMap     = new HashMap<>();
    private ValueEventListener assignedOrdersListener;
    private ValueEventListener profileListener;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_boy_dashboard);

        sessionManager = new SessionManager(this);
        dbRef          = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        uid            = sessionManager.getUid();

        bindViews();
        setupOnlineToggle();
        loadProfile();
        recalculateTodayEarnings();
        loadActiveOrders();
        setupNavCards();
    }

    private void bindViews() {
        chipOnlineToggle    = findViewById(R.id.chipOnlineToggle);
        tvDbName            = findViewById(R.id.tvDbName);
        tvDbRating          = findViewById(R.id.tvDbRating);
        tvDbTodayEarnings   = findViewById(R.id.tvDbTodayEarnings);
        tvDbTotalDeliveries = findViewById(R.id.tvDbTotalDeliveries);
        tvDbTotalEarnings   = findViewById(R.id.tvDbTotalEarnings);
        tvDbMonthEarnings   = findViewById(R.id.tvDbMonthEarnings);
        tvActiveOrdersEmpty = findViewById(R.id.tvActiveOrdersEmpty);
        rvActiveOrders      = findViewById(R.id.rvActiveOrders);
        cardEarnings        = findViewById(R.id.cardEarnings);
        cardProfile         = findViewById(R.id.cardProfile);
        cardHistory         = findViewById(R.id.cardHistory);
        cardRatings         = findViewById(R.id.cardRatings);

        if (tvDbName != null)
            tvDbName.setText("Hello, " + sessionManager.getName() + " 👋");

        if (rvActiveOrders != null) {
            rvActiveOrders.setLayoutManager(new LinearLayoutManager(this));
            rvActiveOrders.setHasFixedSize(false);
        }
    }

    // ── Online toggle (no GPS) ────────────────────────────────────────────────

    private void setupOnlineToggle() {
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_PROFILE).child("isOnline")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean online    = snapshot.getValue(Boolean.class);
                        boolean isOnline  = online != null && online;
                        sessionManager.setOnlineStatus(isOnline);
                        if (chipOnlineToggle != null) {
                            chipOnlineToggle.setChecked(isOnline);
                            updateToggleUI(isOnline);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        boolean isOnline = sessionManager.isOnline();
                        if (chipOnlineToggle != null) {
                            chipOnlineToggle.setChecked(isOnline);
                            updateToggleUI(isOnline);
                        }
                    }
                });

        if (chipOnlineToggle != null) {
            chipOnlineToggle.setOnCheckedChangeListener((btn, isChecked) -> {
                sessionManager.setOnlineStatus(isChecked);
                dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                        .child(Constants.NODE_PROFILE).child("isOnline").setValue(isChecked);
                updateToggleUI(isChecked);
                Toast.makeText(this,
                        isChecked ? "You are now ONLINE 🟢" : "You are now OFFLINE 🔴",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateToggleUI(boolean online) {
        if (chipOnlineToggle == null) return;
        chipOnlineToggle.setText(online ? "🟢  Online" : "🔴  Offline");
        chipOnlineToggle.setChipBackgroundColorResource(
                online ? R.color.statusDelivered : R.color.textSecondary);
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    private void loadProfile() {
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) return;
                Float   rating       = snap.child("avgRating").getValue(Float.class);
                Integer deliveries   = snap.child("totalDeliveries").getValue(Integer.class);
                Double  total        = snap.child("totalEarnings").getValue(Double.class);
                Double  month        = snap.child("monthEarnings").getValue(Double.class);
                Integer totalRatings = snap.child("totalRatings").getValue(Integer.class);

                if (tvDbRating != null) {
                    if (rating != null && rating > 0) {
                        String r = String.format("⭐ %.1f", rating);
                        if (totalRatings != null && totalRatings > 0) r += " (" + totalRatings + ")";
                        tvDbRating.setText(r);
                        tvDbRating.setTextColor(rating >= 4.0f ? 0xFF2E7D32 : rating >= 3.0f ? 0xFFFF8F00 : 0xFFE23744);
                    } else {
                        tvDbRating.setText("⭐ New");
                        tvDbRating.setTextColor(0xFF9E9E9E);
                    }
                }
                if (tvDbTotalDeliveries != null)
                    tvDbTotalDeliveries.setText(String.valueOf(deliveries != null ? deliveries : 0));
                if (tvDbTotalEarnings != null)
                    tvDbTotalEarnings.setText(String.format("₹%.0f", total != null ? total : 0));
                if (tvDbMonthEarnings != null)
                    tvDbMonthEarnings.setText(String.format("₹%.0f", month != null ? month : 0));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_PROFILE).addValueEventListener(profileListener);
    }

    private void recalculateTodayEarnings() {
        long startOfToday = getStartOfTodayMillis();
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_EARNING_HISTORY)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double todayTotal = 0;
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Long   ts      = snap.child("timestamp").getValue(Long.class);
                            Double earning = snap.child("totalEarning").getValue(Double.class);
                            if (ts != null && ts >= startOfToday && earning != null)
                                todayTotal += earning;
                        }
                        if (tvDbTodayEarnings != null)
                            tvDbTodayEarnings.setText(String.format("₹%.0f", todayTotal));
                        double ft = todayTotal;
                        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                                .child(Constants.NODE_PROFILE).child("todayEarnings").setValue(ft);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private long getStartOfTodayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // ── Active Orders ─────────────────────────────────────────────────────────

    private void loadActiveOrders() {
        assignedOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cancelAllTimers();
                activeOrders.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        Order order = snap.getValue(Order.class);
                        if (order != null) {
                            order.setOrderId(snap.getKey());
                            String status = order.getStatus();
                            if (!Constants.STATUS_DELIVERED.equals(status)
                                    && !Constants.STATUS_CANCELLED.equals(status))
                                activeOrders.add(order);
                        }
                    } catch (Exception ignored) {}
                }
                Collections.sort(activeOrders,
                        (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                boolean empty = activeOrders.isEmpty();
                if (tvActiveOrdersEmpty != null)
                    tvActiveOrdersEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (rvActiveOrders != null)
                    rvActiveOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
                setupActiveOrdersAdapter();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_ASSIGNED_ORDERS)
                .addValueEventListener(assignedOrdersListener);
    }

    private void cancelAllTimers() {
        for (CountDownTimer t : timerMap.values()) if (t != null) t.cancel();
        timerMap.clear();
    }

    private void setupActiveOrdersAdapter() {
        if (rvActiveOrders == null) return;
        rvActiveOrders.setAdapter(new RecyclerView.Adapter<ActiveOrderVH>() {

            @NonNull @Override
            public ActiveOrderVH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int type) {
                return new ActiveOrderVH(getLayoutInflater()
                        .inflate(R.layout.item_delivery_active_order, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ActiveOrderVH holder, int position) {
                Order  order          = activeOrders.get(position);
                String orderId        = order.getOrderId();
                String deliveryStatus = order.getDeliveryStatus(); // "PENDING" or "ACCEPTED"

                // Basic info
                if (holder.tvOrderId        != null) holder.tvOrderId.setText(order.getShortOrderId());
                if (holder.tvRestaurantName != null) holder.tvRestaurantName.setText(order.getRestaurantName());
                if (holder.tvAddress        != null) holder.tvAddress.setText(order.getAddress());
                if (holder.tvAmount         != null) holder.tvAmount.setText(String.format("₹%.0f", order.getTotalAmount()));

                String pm = order.getPaymentMethod();
                if (holder.tvPaymentMethod != null) {
                    holder.tvPaymentMethod.setText(pm);
                    holder.tvPaymentMethod.setTextColor(
                            "COD".equalsIgnoreCase(pm) ? 0xFFE65100 : 0xFF2E7D32);
                }

                if (holder.tvTip != null) {
                    if (order.getTipAmount() > 0) {
                        holder.tvTip.setVisibility(View.VISIBLE);
                        holder.tvTip.setText("+ ₹" + (int) order.getTipAmount() + " tip 🎁");
                    } else {
                        holder.tvTip.setVisibility(View.GONE);
                    }
                }

                boolean isPending = "PENDING".equalsIgnoreCase(deliveryStatus);

                if (isPending) {
                    // ── Show Accept/Reject + countdown timer ──────────────────
                    if (holder.layoutAcceptReject != null)
                        holder.layoutAcceptReject.setVisibility(View.VISIBLE);
                    if (holder.tvStatus != null) {
                        holder.tvStatus.setVisibility(View.VISIBLE);
                        holder.tvStatus.setText("⏳ Waiting…");
                        holder.tvStatus.setBackgroundColor(0xFF1565C0);
                    }

                    if (holder.btnAccept != null)
                        holder.btnAccept.setOnClickListener(v -> acceptOrder(order));
                    if (holder.btnReject != null)
                        holder.btnReject.setOnClickListener(v -> rejectOrder(order));

                    // Start timer only if not already running
                    if (!timerMap.containsKey(orderId)) {
                        CountDownTimer timer = new CountDownTimer(60_000, 1000) {
                            public void onTick(long ms) {
                                int secs = (int) (ms / 1000);
                                if (holder.tvStatus != null) {
                                    holder.tvStatus.setText("⏳ " + secs + "s to accept");
                                    holder.tvStatus.setBackgroundColor(
                                            secs <= 10 ? 0xFFE23744 : 0xFF1565C0);
                                }
                            }
                            public void onFinish() {
                                timerMap.remove(orderId);
                                autoRejectOrder(order);
                            }
                        }.start();
                        timerMap.put(orderId, timer);
                    }

                } else {
                    // ── Accepted / In progress — HIDE accept/reject/timer ─────
                    if (holder.layoutAcceptReject != null)
                        holder.layoutAcceptReject.setVisibility(View.GONE);

                    // Cancel any running timer for this order
                    CountDownTimer t = timerMap.remove(orderId);
                    if (t != null) t.cancel();

                    // Show current status badge
                    if (holder.tvStatus != null) {
                        holder.tvStatus.setVisibility(View.VISIBLE);
                        String orderStatus = order.getStatus();
                        String label;
                        int    color;
                        if (Constants.STATUS_PICKED_UP.equals(orderStatus)) {
                            label = "🍱 Food Picked Up";
                            color = 0xFF7B1FA2;
                        } else if (Constants.STATUS_OUT_FOR_DELIVERY.equals(orderStatus)) {
                            label = "🛵 Out for Delivery";
                            color = 0xFF1565C0;
                        } else {
                            label = "✅ Accepted";
                            color = 0xFF2E7D32;
                        }
                        holder.tvStatus.setText(label);
                        holder.tvStatus.setBackgroundColor(color);
                    }
                }

                // Tap row → open detail (only if accepted)
                holder.itemView.setOnClickListener(v -> {
                    if (isPending) {
                        Toast.makeText(DeliveryBoyDashboardActivity.this,
                                "⚠️ Accept order first to view details", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(DeliveryBoyDashboardActivity.this,
                            DeliveryOrderDetailActivity.class);
                    intent.putExtra("orderId", orderId);
                    startActivity(intent);
                });
            }

            @Override public int getItemCount() { return activeOrders.size(); }
        });
    }

    // ── Order actions ─────────────────────────────────────────────────────────

    private void acceptOrder(Order order) {
        // Cancel & remove timer immediately
        CountDownTimer timer = timerMap.remove(order.getOrderId());
        if (timer != null) timer.cancel();

        Map<String, Object> u = new HashMap<>();
        u.put("deliveryStatus", "ACCEPTED");
        u.put("status", Constants.STATUS_PREPARING);

        dbRef.child(Constants.NODE_ORDERS).child(order.getOrderId()).updateChildren(u);
        if (order.getUserId() != null)
            dbRef.child(Constants.NODE_USERS).child(order.getUserId())
                    .child(Constants.NODE_MY_ORDERS).child(order.getOrderId()).updateChildren(u);
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_ASSIGNED_ORDERS).child(order.getOrderId()).updateChildren(u);

        Toast.makeText(this, "Order Accepted ✅", Toast.LENGTH_SHORT).show();

        // Navigate to detail for next steps
        Intent intent = new Intent(this, DeliveryOrderDetailActivity.class);
        intent.putExtra("orderId", order.getOrderId());
        startActivity(intent);
    }

    private void rejectOrder(Order order) {
        CountDownTimer timer = timerMap.remove(order.getOrderId());
        if (timer != null) timer.cancel();

        Map<String, Object> u = new HashMap<>();
        u.put("deliveryStatus", "REJECTED");
        u.put("deliveryBoyId", "");
        u.put("deliveryBoyName", "");
        u.put("status", Constants.STATUS_PENDING);
        dbRef.child(Constants.NODE_ORDERS).child(order.getOrderId()).updateChildren(u);
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_ASSIGNED_ORDERS).child(order.getOrderId()).removeValue();
        Toast.makeText(this, "Order Rejected ❌", Toast.LENGTH_SHORT).show();
    }

    private void autoRejectOrder(Order order) {
        Map<String, Object> u = new HashMap<>();
        u.put("deliveryStatus", "REJECTED");
        u.put("deliveryBoyId", "");
        u.put("deliveryBoyName", "");
        u.put("status", Constants.STATUS_PENDING);
        dbRef.child(Constants.NODE_ORDERS).child(order.getOrderId()).updateChildren(u);
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_ASSIGNED_ORDERS).child(order.getOrderId()).removeValue();
        runOnUiThread(() -> Toast.makeText(this,
                "⏰ Auto-rejected: " + order.getShortOrderId(), Toast.LENGTH_SHORT).show());
    }

    // ── Nav cards ─────────────────────────────────────────────────────────────

    private void setupNavCards() {
        if (cardEarnings != null)
            cardEarnings.setOnClickListener(v ->
                    startActivity(new Intent(this, DeliveryEarningsActivity.class)));
        if (cardProfile != null)
            cardProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, DeliveryBoyProfileActivity.class)));
        if (cardHistory != null)
            cardHistory.setOnClickListener(v ->
                    startActivity(new Intent(this, DeliveryOrderHistoryActivity.class)));
        if (cardRatings != null)
            cardRatings.setOnClickListener(v ->
                    startActivity(new Intent(this, DeliveryBoyRatingsActivity.class)));

        View btnLogout = findViewById(R.id.btnDbLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                            .setTitle("Logout")
                            .setMessage("Are you sure you want to logout?")
                            .setPositiveButton("Logout", (d, w) -> {


                                sessionManager.logout();
                                Intent intent = new Intent(this, AuthActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show());
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ActiveOrderVH extends RecyclerView.ViewHolder {
        TextView       tvOrderId, tvRestaurantName, tvAddress, tvAmount,
                tvStatus, tvPaymentMethod, tvTip;
        MaterialButton btnAccept, btnReject;
        View           layoutAcceptReject;

        ActiveOrderVH(@NonNull android.view.View v) {
            super(v);
            tvOrderId         = v.findViewById(R.id.tvActiveOrderId);
            tvRestaurantName  = v.findViewById(R.id.tvActiveRestaurantName);
            tvAddress         = v.findViewById(R.id.tvActiveAddress);
            tvAmount          = v.findViewById(R.id.tvActiveAmount);
            tvStatus          = v.findViewById(R.id.tvActiveStatus);
            tvPaymentMethod   = v.findViewById(R.id.tvActivePaymentMethod);
            tvTip             = v.findViewById(R.id.tvActiveTip);
            btnAccept         = v.findViewById(R.id.btnAccept);
            btnReject         = v.findViewById(R.id.btnReject);
            layoutAcceptReject = v.findViewById(R.id.layoutAcceptReject);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAllTimers();
        if (assignedOrdersListener != null && uid != null)
            dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                    .child(Constants.NODE_ASSIGNED_ORDERS)
                    .removeEventListener(assignedOrdersListener);
        if (profileListener != null && uid != null)
            dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                    .child(Constants.NODE_PROFILE)
                    .removeEventListener(profileListener);
    }
}