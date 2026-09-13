package com.example.fooddeliveryapp.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.Order;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DeliveryOrderHistoryActivity — shows delivered + cancelled orders for this delivery boy.
 * Status band at top of each card is colored green (delivered) or red (cancelled).
 */
public class DeliveryOrderHistoryActivity extends AppCompatActivity {

    private RecyclerView       rvOrderHistory;
    private TextView           tvNoHistory;
    private final List<Order>  historyList = new ArrayList<>();
    private DatabaseReference  dbRef;
    private SessionManager     sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_order_history);

        dbRef          = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        sessionManager = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbarOrderHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Order History");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvNoHistory    = findViewById(R.id.tvNoOrderHistory);
        rvOrderHistory = findViewById(R.id.rvOrderHistory);
        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));

        loadOrderHistory();
    }

    private void loadOrderHistory() {
        String uid = sessionManager.getUid();
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_ASSIGNED_ORDERS)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            try {
                                Order order = snap.getValue(Order.class);
                                if (order != null) {
                                    order.setOrderId(snap.getKey());
                                    String status = order.getStatus();
                                    if (Constants.STATUS_DELIVERED.equals(status)
                                            || Constants.STATUS_CANCELLED.equals(status)) {
                                        historyList.add(order);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }

                        Collections.sort(historyList,
                                (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                        boolean empty = historyList.isEmpty();
                        tvNoHistory.setVisibility(empty ? View.VISIBLE : View.GONE);
                        rvOrderHistory.setVisibility(empty ? View.GONE : View.VISIBLE);
                        rvOrderHistory.setAdapter(new HistoryAdapter());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        Toast.makeText(DeliveryOrderHistoryActivity.this,
                                "Failed to load history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── History adapter ───────────────────────────────────────────────────────
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryVH> {

        @NonNull
        @Override
        public HistoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_delivery_history_order, parent, false);
            return new HistoryVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryVH h, int position) {
            Order order = historyList.get(position);

            boolean delivered = Constants.STATUS_DELIVERED.equals(order.getStatus());

            // Color band at top of card
            h.viewStatusBand.setBackgroundColor(
                    delivered ? 0xFF2E7D32 : 0xFFE23744);

            h.tvHistOrderId.setText(order.getShortOrderId());

            h.tvHistRestaurant.setText(order.getRestaurantName() != null
                    ? order.getRestaurantName() : "—");

            h.tvHistCustomer.setText(order.getUserName() != null
                    ? order.getUserName() : "—");

            h.tvHistAmount.setText(String.format(Locale.getDefault(),
                    "₹%.0f", order.getTotalAmount()));

            h.tvHistDate.setText(new SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.getDefault())
                    .format(new Date(order.getTimestamp())));

            // Status label
            h.tvHistStatus.setText(delivered ? "✅ Delivered" : "❌ Cancelled");
            h.tvHistStatus.setTextColor(delivered ? 0xFF2E7D32 : 0xFFE23744);
            h.tvHistStatus.setBackgroundColor(delivered ? 0xFFE8F5E9 : 0xFFFFEBEE);

            // Tip
            if (order.getTipAmount() > 0) {
                h.tvHistTip.setVisibility(View.VISIBLE);
                h.tvHistTip.setText("🎁 ₹" + (int) order.getTipAmount() + " tip");
            } else {
                h.tvHistTip.setVisibility(View.GONE);
            }

            // Approx earning (only for delivered)
            if (delivered) {
                double commission = Constants.BASE_DELIVERY_FEE
                        + (order.getTotalAmount() * Constants.DELIVERY_COMMISSION_PERCENT / 100.0);
                double total = commission + order.getTipAmount();
                h.tvHistEarning.setVisibility(View.VISIBLE);
                h.tvHistEarning.setText(String.format(Locale.getDefault(), "Earned ~₹%.0f", total));
            } else {
                h.tvHistEarning.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return historyList.size(); }

        class HistoryVH extends RecyclerView.ViewHolder {
            View     viewStatusBand;
            TextView tvHistOrderId, tvHistRestaurant, tvHistCustomer,
                    tvHistAmount, tvHistDate, tvHistStatus, tvHistTip, tvHistEarning;

            HistoryVH(@NonNull View v) {
                super(v);
                viewStatusBand   = v.findViewById(R.id.viewHistStatusBand);
                tvHistOrderId    = v.findViewById(R.id.tvHistOrderId);
                tvHistRestaurant = v.findViewById(R.id.tvHistRestaurant);
                tvHistCustomer   = v.findViewById(R.id.tvHistCustomer);
                tvHistAmount     = v.findViewById(R.id.tvHistAmount);
                tvHistDate       = v.findViewById(R.id.tvHistDate);
                tvHistStatus     = v.findViewById(R.id.tvHistStatus);
                tvHistTip        = v.findViewById(R.id.tvHistTip);
                tvHistEarning    = v.findViewById(R.id.tvHistEarning);
            }
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}