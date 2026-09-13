package com.example.fooddeliveryapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.databinding.ActivityAdminReviewsBinding;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AdminReviewsActivity — admin sees all reviews with sub-ratings + photos.
 * ✅ Shows review photos (Base64) — visible to admin AND users
 * ✅ Shows delivery boy rating
 * ✅ Summary card with avg rating + color coding
 * ✅ Delete review
 */
public class AdminReviewsActivity extends AppCompatActivity {

    private ActivityAdminReviewsBinding binding;
    private DatabaseReference dbRef;
    private String adminId;
    private ReviewsAdapter reviewsAdapter;
    private final List<ReviewItem> reviewList = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────

    static class ReviewItem {
        String reviewId, userName, comment, orderId;
        String deliveryBoyName;
        float  ratingFood, ratingDelivery, ratingPacking, ratingOverall, ratingDeliveryBoy;
        long   timestamp;
        String reviewImageBase64;

        ReviewItem(String reviewId, String userName, String comment,
                   float ratingFood, float ratingDelivery, float ratingPacking,
                   float ratingOverall, float ratingDeliveryBoy,
                   long timestamp, String orderId,
                   String deliveryBoyName, String reviewImageBase64) {
            this.reviewId          = reviewId;
            this.userName          = userName;
            this.comment           = comment;
            this.ratingFood        = ratingFood;
            this.ratingDelivery    = ratingDelivery;
            this.ratingPacking     = ratingPacking;
            this.ratingOverall     = ratingOverall;
            this.ratingDeliveryBoy = ratingDeliveryBoy;
            this.timestamp         = timestamp;
            this.orderId           = orderId;
            this.deliveryBoyName   = deliveryBoyName;
            this.reviewImageBase64 = reviewImageBase64;
        }
    }

    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adminId = new SessionManager(this).getUid();
        dbRef   = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Reviews & Feedback ⭐");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        loadReviews();
    }

    private void setupRecyclerView() {
        reviewsAdapter = new ReviewsAdapter(reviewList, this::deleteReview);
        binding.rvReviews.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReviews.setAdapter(reviewsAdapter);
    }

    private void loadReviews() {
        binding.progressBar.setVisibility(View.VISIBLE);

        dbRef.child("Reviews").child(adminId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        reviewList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String reviewId        = snap.getKey();
                            String userName        = snap.child("userName").getValue(String.class);
                            String comment         = snap.child("comment").getValue(String.class);
                            Float  ratingFood      = snap.child("ratingFood").getValue(Float.class);
                            Float  ratingDelivery  = snap.child("ratingDelivery").getValue(Float.class);
                            Float  ratingPacking   = snap.child("ratingPacking").getValue(Float.class);
                            Float  ratingOverall   = snap.child("ratingOverall").getValue(Float.class);
                            Float  ratingDb        = snap.child("ratingDeliveryBoy").getValue(Float.class);
                            Long   ts              = snap.child("timestamp").getValue(Long.class);
                            String orderId         = snap.child("orderId").getValue(String.class);
                            String dbName          = snap.child("deliveryBoyName").getValue(String.class);
                            // ✅ Read review image
                            String imgBase64       = snap.child("reviewImageBase64").getValue(String.class);

                            reviewList.add(new ReviewItem(
                                    reviewId       != null ? reviewId       : "",
                                    userName       != null ? userName       : "Anonymous",
                                    comment        != null ? comment        : "",
                                    ratingFood     != null ? ratingFood     : 0f,
                                    ratingDelivery != null ? ratingDelivery : 0f,
                                    ratingPacking  != null ? ratingPacking  : 0f,
                                    ratingOverall  != null ? ratingOverall  : 0f,
                                    ratingDb       != null ? ratingDb       : 0f,
                                    ts             != null ? ts             : 0L,
                                    orderId        != null ? orderId        : "",
                                    dbName         != null ? dbName         : "",
                                    imgBase64      != null ? imgBase64      : ""
                            ));
                        }

                        // Sort newest first
                        reviewList.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

                        binding.progressBar.setVisibility(View.GONE);
                        boolean empty = reviewList.isEmpty();
                        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                        binding.rvReviews.setVisibility(empty ? View.GONE : View.VISIBLE);
                        updateSummary(reviewList);
                        reviewsAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void updateSummary(List<ReviewItem> reviews) {
        if (reviews.isEmpty()) {
            binding.tvAvgRating.setText("—");
            binding.tvAvgRating.setTextColor(0xFFFFFFFF);
            binding.tvTotalReviews.setText("0 reviews");
            return;
        }
        float sum = 0;
        for (ReviewItem r : reviews) sum += r.ratingOverall;
        float avg = sum / reviews.size();

        binding.tvAvgRating.setText(String.format(Locale.getDefault(), "%.1f ⭐", avg));
        if      (avg >= 3.5f) binding.tvAvgRating.setTextColor(0xFF4CAF50);
        else if (avg >= 2.0f) binding.tvAvgRating.setTextColor(0xFFFFB300);
        else                  binding.tvAvgRating.setTextColor(0xFFE23744);

        binding.tvTotalReviews.setText(
                reviews.size() + " review" + (reviews.size() != 1 ? "s" : ""));
    }

    private void deleteReview(ReviewItem review) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Review")
                .setMessage("Remove this review by " + review.userName + "?")
                .setPositiveButton("Delete", (d, w) ->
                        dbRef.child("Reviews").child(adminId).child(review.reviewId).removeValue()
                                .addOnSuccessListener(u ->
                                        android.widget.Toast.makeText(this,
                                                "Review removed", android.widget.Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    interface OnDeleteReview { void onDelete(ReviewItem r); }

    static class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.VH> {
        private final List<ReviewItem> list;
        private final OnDeleteReview   onDelete;

        ReviewsAdapter(List<ReviewItem> list, OnDeleteReview onDelete) {
            this.list = list; this.onDelete = onDelete;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_review_admin, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ReviewItem r = list.get(pos);

            h.tvUser.setText(!r.userName.isEmpty()
                    ? String.valueOf(r.userName.charAt(0)).toUpperCase() : "?");
            h.tvUserName.setText(r.userName);

            // Overall
            String overallText = String.format(Locale.getDefault(), "%.1f ⭐", r.ratingOverall);
            h.tvOverallRating.setText(overallText);
            if      (r.ratingOverall >= 3.5f) h.tvOverallRating.setTextColor(0xFF4CAF50);
            else if (r.ratingOverall >= 2.0f) h.tvOverallRating.setTextColor(0xFFFFB300);
            else                              h.tvOverallRating.setTextColor(0xFFE23744);

            // Sub-ratings
            h.tvFoodRating.setText("🍽️ Food: " + getStars(r.ratingFood));
            h.tvDeliveryRating.setText("🛵 Delivery: " + getStars(r.ratingDelivery));
            h.tvPackingRating.setText("📦 Packing: " + getStars(r.ratingPacking));

            // Delivery boy rating
            if (r.ratingDeliveryBoy > 0 && h.tvDeliveryBoyRating != null) {
                h.tvDeliveryBoyRating.setVisibility(View.VISIBLE);
                h.tvDeliveryBoyRating.setText("🏍️ Rider " + (r.deliveryBoyName.isEmpty()
                        ? "" : "(" + r.deliveryBoyName + ")") + ": " + getStars(r.ratingDeliveryBoy));
            } else if (h.tvDeliveryBoyRating != null) {
                h.tvDeliveryBoyRating.setVisibility(View.GONE);
            }

            // Comment
            h.tvComment.setText(r.comment.isEmpty() ? "No comment left" : r.comment);

            // Date
            if (r.timestamp > 0) {
                h.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(new Date(r.timestamp)));
            }

            // Order ref
            if (!r.orderId.isEmpty()) {
                String shortId = r.orderId.length() > 8
                        ? "#" + r.orderId.substring(r.orderId.length() - 8).toUpperCase()
                        : "#" + r.orderId;
                h.tvOrderRef.setText("Order: " + shortId);
                h.tvOrderRef.setVisibility(View.VISIBLE);
            } else {
                h.tvOrderRef.setVisibility(View.GONE);
            }

            // ✅ Review photo — show if present, hide if not
            if (r.reviewImageBase64 != null && !r.reviewImageBase64.isEmpty()
                    && h.ivReviewPhoto != null) {
                try {
                    byte[] bytes = Base64.decode(r.reviewImageBase64, Base64.DEFAULT);
                    Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp != null) {
                        h.cardReviewPhoto.setVisibility(View.VISIBLE);
                        h.ivReviewPhoto.setVisibility(View.VISIBLE);
                        h.ivReviewPhoto.setImageBitmap(bmp);
                    } else {
                        h.cardReviewPhoto.setVisibility(View.GONE);
                        h.ivReviewPhoto.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    h.cardReviewPhoto.setVisibility(View.GONE);
                    h.ivReviewPhoto.setVisibility(View.GONE);
                }
            } else {
                if (h.cardReviewPhoto != null) h.cardReviewPhoto.setVisibility(View.GONE);
                if (h.ivReviewPhoto != null)   h.ivReviewPhoto.setVisibility(View.GONE);
            }

            h.btnDelete.setOnClickListener(v -> onDelete.onDelete(r));
        }

        private String getStars(float rating) {
            int r = Math.round(rating);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= 5; i++) sb.append(i <= r ? "⭐" : "☆");
            return sb.toString() + String.format(Locale.getDefault(), " (%.1f)", rating);
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvUser, tvUserName, tvOverallRating;
            TextView tvFoodRating, tvDeliveryRating, tvPackingRating, tvDeliveryBoyRating;
            TextView tvComment, tvDate, tvOrderRef;
            ImageView ivReviewPhoto;
            com.google.android.material.card.MaterialCardView cardReviewPhoto;
            com.google.android.material.button.MaterialButton btnDelete;

            VH(@NonNull View v) {
                super(v);
                tvUser             = v.findViewById(R.id.tvReviewUser);
                tvUserName         = v.findViewById(R.id.tvReviewUserName);
                tvOverallRating    = v.findViewById(R.id.tvReviewRating);
                tvFoodRating       = v.findViewById(R.id.tvFoodRating);
                tvDeliveryRating   = v.findViewById(R.id.tvDeliveryRating);
                tvPackingRating    = v.findViewById(R.id.tvPackingRating);
                tvDeliveryBoyRating= v.findViewById(R.id.tvDeliveryBoyRating);
                tvComment          = v.findViewById(R.id.tvReviewComment);
                tvDate             = v.findViewById(R.id.tvReviewDate);
                tvOrderRef         = v.findViewById(R.id.tvOrderRef);
                ivReviewPhoto      = v.findViewById(R.id.ivAdminReviewPhoto);
                cardReviewPhoto    = v.findViewById(R.id.cardAdminReviewPhoto);
                btnDelete          = v.findViewById(R.id.btnDeleteReview);
            }
        }
    }
}