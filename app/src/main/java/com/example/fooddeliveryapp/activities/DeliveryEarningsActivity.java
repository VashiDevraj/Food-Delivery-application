package com.example.fooddeliveryapp.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.DeliveryEarning;
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
 * DeliveryEarningsActivity
 * Shows: Today / Month / Total earnings + full earning history list
 */
public class DeliveryEarningsActivity extends AppCompatActivity {

    private TextView tvTodayEarning, tvMonthEarning, tvTotalEarning,
            tvTotalDeliveries, tvNoEarnings;
    private RecyclerView rvEarningHistory;

    private final List<DeliveryEarning> earningList = new ArrayList<>();
    private DatabaseReference dbRef;
    private SessionManager    sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_earnings);

        dbRef          = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        sessionManager = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbarEarnings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Earnings");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTodayEarning    = findViewById(R.id.tvTodayEarning);
        tvMonthEarning    = findViewById(R.id.tvMonthEarning);
        tvTotalEarning    = findViewById(R.id.tvTotalEarningAll);
        tvTotalDeliveries = findViewById(R.id.tvTotalDeliveriesEarnings);
        tvNoEarnings      = findViewById(R.id.tvNoEarnings);
        rvEarningHistory  = findViewById(R.id.rvEarningHistory);

        rvEarningHistory.setLayoutManager(new LinearLayoutManager(this));

        loadStats();
        loadEarningHistory();
    }

    private void loadStats() {
        String uid = sessionManager.getUid();
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_PROFILE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        Double today  = snap.child("todayEarnings").getValue(Double.class);
                        Double month  = snap.child("monthEarnings").getValue(Double.class);
                        Double total  = snap.child("totalEarnings").getValue(Double.class);
                        Integer deliv = snap.child("totalDeliveries").getValue(Integer.class);

                        tvTodayEarning.setText(String.format("₹%.0f", today != null ? today : 0));
                        tvMonthEarning.setText(String.format("₹%.0f", month != null ? month : 0));
                        tvTotalEarning.setText(String.format("₹%.0f", total != null ? total : 0));
                        tvTotalDeliveries.setText(String.valueOf(deliv != null ? deliv : 0));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadEarningHistory() {
        String uid = sessionManager.getUid();
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_EARNING_HISTORY)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        earningList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            try {
                                DeliveryEarning e = snap.getValue(DeliveryEarning.class);
                                if (e != null) earningList.add(e);
                            } catch (Exception ignored) {}
                        }
                        Collections.sort(earningList,
                                (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                        boolean empty = earningList.isEmpty();
                        tvNoEarnings.setVisibility(empty ? View.VISIBLE : View.GONE);
                        rvEarningHistory.setVisibility(empty ? View.GONE : View.VISIBLE);

                        rvEarningHistory.setAdapter(new EarningAdapter());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Earning history adapter ───────────────────────────────────────────────
    class EarningAdapter extends RecyclerView.Adapter<EarningAdapter.EarningVH> {

        @NonNull
        @Override
        public EarningVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_earning_history, parent, false);
            return new EarningVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull EarningVH h, int position) {
            DeliveryEarning e = earningList.get(position);
            h.tvEarningOrderId.setText(e.getShortOrderId());
            h.tvEarningRestaurant.setText(e.getRestaurantName());
            h.tvEarningDate.setText(new SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.getDefault())
                    .format(new Date(e.getTimestamp())));
            h.tvEarningTotal.setText(String.format("+ ₹%.0f", e.getTotalEarning()));
            h.tvEarningBreakdown.setText(
                    String.format("Base ₹%.0f  +  Tip ₹%.0f", e.getCommission(), e.getTipAmount()));
        }

        @Override public int getItemCount() { return earningList.size(); }

        class EarningVH extends RecyclerView.ViewHolder {
            TextView tvEarningOrderId, tvEarningRestaurant, tvEarningDate,
                    tvEarningTotal, tvEarningBreakdown;

            EarningVH(@NonNull View v) {
                super(v);
                tvEarningOrderId    = v.findViewById(R.id.tvEarningOrderId);
                tvEarningRestaurant = v.findViewById(R.id.tvEarningRestaurant);
                tvEarningDate       = v.findViewById(R.id.tvEarningDate);
                tvEarningTotal      = v.findViewById(R.id.tvEarningTotal);
                tvEarningBreakdown  = v.findViewById(R.id.tvEarningBreakdown);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}