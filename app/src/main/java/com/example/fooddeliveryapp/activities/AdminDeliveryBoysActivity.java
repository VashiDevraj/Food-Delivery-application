package com.example.fooddeliveryapp.activities;

import android.content.Intent;
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
import com.example.fooddeliveryapp.models.DeliveryBoy;
import com.example.fooddeliveryapp.utils.Constants;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminDeliveryBoysActivity
 * ✅ Online boys shown first, sorted by rating desc; offline last
 * ✅ Prominently shows rating for each delivery boy
 * ✅ Cannot tap assign for offline boys (visual indicator)
 * ✅ Status bar color: green=online, grey=offline
 */
public class AdminDeliveryBoysActivity extends AppCompatActivity {

    private RecyclerView            rvDeliveryBoys;
    private TextView                tvNoBoys, tvTotalBoys;
    private final List<DeliveryBoy> boyList = new ArrayList<>();
    private DatabaseReference       dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_delivery_boys);

        dbRef = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();

        Toolbar toolbar = findViewById(R.id.toolbarAdminDb);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Delivery Partners");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvNoBoys       = findViewById(R.id.tvNoDeliveryBoys);
        tvTotalBoys    = findViewById(R.id.tvTotalBoys);
        rvDeliveryBoys = findViewById(R.id.rvDeliveryBoys);
        rvDeliveryBoys.setLayoutManager(new LinearLayoutManager(this));

        loadDeliveryBoys();
    }

    private void loadDeliveryBoys() {
        dbRef.child(Constants.NODE_DELIVERY_BOYS)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boyList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            try {
                                DataSnapshot profileSnap = snap.child(Constants.NODE_PROFILE);

                                DeliveryBoy boy = profileSnap.getValue(DeliveryBoy.class);

                                if (boy != null) {
                                    boy.setUid(snap.getKey());

                                    // 🔥 FIX
                                    Boolean onlineVal = profileSnap.child("isOnline").getValue(Boolean.class);
                                    boy.setOnline(onlineVal != null && onlineVal);


                                }
                                if (boy != null) {
                                    boy.setUid(snap.getKey());
                                    boyList.add(boy);
                                }
                            } catch (Exception ignored) {}
                        }

                        // ✅ Online first (highest rating desc), offline last
                        boyList.sort((a, b) -> {
                            if (a.isOnline() != b.isOnline()) return a.isOnline() ? -1 : 1;
                            return Float.compare(b.getAvgRating(), a.getAvgRating());
                        });

                        boolean empty = boyList.isEmpty();
                        tvNoBoys.setVisibility(empty ? View.VISIBLE : View.GONE);
                        rvDeliveryBoys.setVisibility(empty ? View.GONE : View.VISIBLE);

                        int online = 0;
                        for (DeliveryBoy b : boyList) if (b.isOnline()) online++;
                        tvTotalBoys.setText("Total: " + boyList.size()
                                + "  •  Online: " + online + "  🟢");

                        rvDeliveryBoys.setAdapter(new DeliveryBoyAdapter());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        Toast.makeText(AdminDeliveryBoysActivity.this,
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Inner adapter ─────────────────────────────────────────────────────────
    class DeliveryBoyAdapter extends RecyclerView.Adapter<DeliveryBoyAdapter.BoyVH> {

        @NonNull
        @Override
        public BoyVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_delivery_boy, parent, false);
            return new BoyVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BoyVH h, int position) {
            DeliveryBoy boy = boyList.get(position);
            boolean isOnline = boy.isOnline();

            // Color bar: green=online, grey=offline
            h.viewStatusBar.setBackgroundColor(isOnline ? 0xFF4CAF50 : 0xFFBDBDBD);

            // Name with vehicle icon
            h.tvBoyName.setText(boy.getVehicleIcon() + " " + boy.getName());
            h.tvBoyName.setAlpha(isOnline ? 1.0f : 0.6f);

            // Vehicle info
            String vehicleInfo = boy.getVehicleType();
            if (boy.getVehicleNumber() != null && !boy.getVehicleNumber().isEmpty())
                vehicleInfo += " · " + boy.getVehicleNumber();
            h.tvBoyVehicle.setText(vehicleInfo);

            // ✅ Rating prominently displayed
            if (boy.getAvgRating() > 0) {
                h.tvBoyRating.setText(String.format("⭐ %.1f  (%d ratings)",
                        boy.getAvgRating(), boy.getTotalRatings()));
                // Color code rating
                if      (boy.getAvgRating() >= 4.0f) h.tvBoyRating.setTextColor(0xFF2E7D32);
                else if (boy.getAvgRating() >= 3.0f) h.tvBoyRating.setTextColor(0xFFFF8F00);
                else                                  h.tvBoyRating.setTextColor(0xFFE23744);
            } else {
                h.tvBoyRating.setText("⭐ New Partner");
                h.tvBoyRating.setTextColor(0xFF757575);
            }

            // Deliveries
            h.tvBoyDeliveries.setText("📦 " + boy.getTotalDeliveries() + " deliveries");

            // Earnings
            h.tvBoyEarnings.setText("💰 ₹" + String.format("%.0f", boy.getTotalEarnings()));

            // Phone
            h.tvBoyPhone.setText("📞 " + (boy.getPhone() != null && !boy.getPhone().isEmpty()
                    ? boy.getPhone() : "N/A"));

            // Status chip
            h.chipStatus.setText(isOnline ? "🟢 Online" : "🔴 Offline");
            h.chipStatus.setChipBackgroundColorResource(
                    isOnline ? R.color.statusDelivered : R.color.textSecondary);

            // Card alpha for offline
            h.itemView.setAlpha(isOnline ? 1.0f : 0.70f);
        }

        @Override public int getItemCount() { return boyList.size(); }

        class BoyVH extends RecyclerView.ViewHolder {
            View     viewStatusBar;
            TextView tvBoyName, tvBoyVehicle, tvBoyRating,
                    tvBoyDeliveries, tvBoyEarnings, tvBoyPhone;
            Chip     chipStatus;

            BoyVH(@NonNull View v) {
                super(v);
                viewStatusBar   = v.findViewById(R.id.viewStatusBar);
                tvBoyName       = v.findViewById(R.id.tvBoyName);
                tvBoyVehicle    = v.findViewById(R.id.tvBoyVehicle);
                tvBoyRating     = v.findViewById(R.id.tvBoyRating);
                tvBoyDeliveries = v.findViewById(R.id.tvBoyDeliveries);
                tvBoyEarnings   = v.findViewById(R.id.tvBoyEarnings);
                tvBoyPhone      = v.findViewById(R.id.tvBoyPhone);
                chipStatus      = v.findViewById(R.id.chipBoyStatus);
            }
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}