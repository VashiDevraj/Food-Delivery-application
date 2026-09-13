package com.example.fooddeliveryapp.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
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
 * DeliveryBoyRatingsActivity
 *
 * NEW SCREEN — Delivery boy sees all ratings given by users:
 * ✅ Overall avg rating with star bar
 * ✅ Rating breakdown: 5★ to 1★ counts
 * ✅ Per-review card: reviewer name, star rating, comment, date, order ID
 * ✅ Empty state when no ratings
 * ✅ Newest-first sort
 */
public class DeliveryBoyRatingsActivity extends AppCompatActivity {

    private TextView      tvAvgRating, tvTotalRatings, tvEmptyRatings;
    private TextView      tvStar5, tvStar4, tvStar3, tvStar2, tvStar1;
    private RecyclerView  rvRatings;

    private DatabaseReference dbRef;
    private SessionManager    sessionManager;
    private String            uid;

    private final List<RatingRecord> records = new ArrayList<>();

    static class RatingRecord {
        String reviewerName, comment, orderId;
        float  rating;
        long   timestamp;

        RatingRecord(String n, float r, String c, long ts, String oid) {
            reviewerName = n; rating = r; comment = c; timestamp = ts; orderId = oid;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_boy_ratings);

        dbRef          = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        sessionManager = new SessionManager(this);
        uid            = sessionManager.getUid();

        Toolbar toolbar = findViewById(R.id.toolbarDbRatings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Ratings ⭐");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvAvgRating    = findViewById(R.id.tvRatingsAvg);
        tvTotalRatings = findViewById(R.id.tvRatingsTotalCount);
        tvEmptyRatings = findViewById(R.id.tvRatingsEmpty);
        tvStar5        = findViewById(R.id.tvRatingStar5);
        tvStar4        = findViewById(R.id.tvRatingStar4);
        tvStar3        = findViewById(R.id.tvRatingStar3);
        tvStar2        = findViewById(R.id.tvRatingStar2);
        tvStar1        = findViewById(R.id.tvRatingStar1);
        rvRatings      = findViewById(R.id.rvMyRatings);
        rvRatings.setLayoutManager(new LinearLayoutManager(this));

        loadRatings();
    }

    private void loadRatings() {
        dbRef.child(Constants.NODE_DELIVERY_BOYS).child(uid)
                .child(Constants.NODE_RATINGS)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        records.clear();
                        float sum   = 0;
                        int   count = 0;
                        int[] stars = {0, 0, 0, 0, 0}; // 0=1★ ... 4=5★

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String name    = snap.child("userName").getValue(String.class);
                            Float  rating  = snap.child("rating").getValue(Float.class);
                            String comment = snap.child("comment").getValue(String.class);
                            Long   ts      = snap.child("timestamp").getValue(Long.class);
                            String ordId   = snap.child("orderId").getValue(String.class);

                            if (rating == null) continue;

                            records.add(new RatingRecord(
                                    name != null ? name : "Anonymous",
                                    rating,
                                    comment != null ? comment : "",
                                    ts != null ? ts : 0,
                                    ordId != null ? ordId : ""
                            ));

                            sum += rating;
                            count++;

                            int star = Math.round(rating);
                            if (star >= 1 && star <= 5) stars[star - 1]++;
                        }

                        // Newest first
                        Collections.sort(records, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                        // Update header
                        boolean hasData = count > 0;
                        if (tvEmptyRatings != null)
                            tvEmptyRatings.setVisibility(hasData ? View.GONE : View.VISIBLE);

                        if (tvAvgRating != null) {
                            if (hasData) {
                                float avg = sum / count;
                                tvAvgRating.setText(String.format("%.1f ⭐", avg));
                                if      (avg >= 4.0f) tvAvgRating.setTextColor(0xFF2E7D32);
                                else if (avg >= 3.0f) tvAvgRating.setTextColor(0xFFFF8F00);
                                else                  tvAvgRating.setTextColor(0xFFE23744);
                            } else {
                                tvAvgRating.setText("- ⭐");
                            }
                        }
                        if (tvTotalRatings != null)
                            tvTotalRatings.setText(count + " ratings");

                        // Star breakdown
                        if (tvStar5 != null) tvStar5.setText("5★  " + stars[4]);
                        if (tvStar4 != null) tvStar4.setText("4★  " + stars[3]);
                        if (tvStar3 != null) tvStar3.setText("3★  " + stars[2]);
                        if (tvStar2 != null) tvStar2.setText("2★  " + stars[1]);
                        if (tvStar1 != null) tvStar1.setText("1★  " + stars[0]);

                        updateAdapter();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void updateAdapter() {
        rvRatings.setAdapter(new RecyclerView.Adapter<RatingVH>() {
            @NonNull @Override
            public RatingVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_delivery_boy_rating_card, parent, false);
                return new RatingVH(v);
            }

            @Override
            public void onBindViewHolder(@NonNull RatingVH holder, int pos) {
                RatingRecord record = records.get(pos);

                holder.tvReviewerName.setText(record.reviewerName);
                holder.rbRating.setRating(record.rating);
                holder.tvRatingValue.setText(String.format("%.1f", record.rating));

                // Color code rating value
                if      (record.rating >= 4.0f) holder.tvRatingValue.setTextColor(0xFF2E7D32);
                else if (record.rating >= 3.0f) holder.tvRatingValue.setTextColor(0xFFFF8F00);
                else                            holder.tvRatingValue.setTextColor(0xFFE23744);

                // Comment
                if (!record.comment.isEmpty()) {
                    holder.tvComment.setVisibility(View.VISIBLE);
                    holder.tvComment.setText(record.comment);
                } else {
                    holder.tvComment.setVisibility(View.GONE);
                }

                // Date
                if (record.timestamp > 0) {
                    holder.tvDate.setText(
                            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    .format(new Date(record.timestamp)));
                }

                // Order short ID
                if (!record.orderId.isEmpty()) {
                    String shortId = record.orderId.length() > 8
                            ? "#" + record.orderId.substring(record.orderId.length() - 8).toUpperCase()
                            : "#" + record.orderId.toUpperCase();
                    holder.tvOrderRef.setText(shortId);
                    holder.tvOrderRef.setVisibility(View.VISIBLE);
                } else {
                    holder.tvOrderRef.setVisibility(View.GONE);
                }
            }

            @Override public int getItemCount() { return records.size(); }
        });
    }

    static class RatingVH extends RecyclerView.ViewHolder {
        TextView  tvReviewerName, tvRatingValue, tvComment, tvDate, tvOrderRef;
        RatingBar rbRating;

        RatingVH(@NonNull View v) {
            super(v);
            tvReviewerName = v.findViewById(R.id.tvRatingReviewerName);
            rbRating       = v.findViewById(R.id.rbRatingBar);
            tvRatingValue  = v.findViewById(R.id.tvRatingValue);
            tvComment      = v.findViewById(R.id.tvRatingComment);
            tvDate         = v.findViewById(R.id.tvRatingDate);
            tvOrderRef     = v.findViewById(R.id.tvRatingOrderRef);
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
