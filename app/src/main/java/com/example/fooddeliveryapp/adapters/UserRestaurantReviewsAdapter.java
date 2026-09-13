package com.example.fooddeliveryapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * UserRestaurantReviewsAdapter
 * Shows reviews in the RestaurantMenuActivity "Reviews" section for regular users.
 * ✅ Displays review photos (Base64) — shown only when present
 * ✅ Shows sub-ratings: food, delivery, packing
 * ✅ Overall rating with color coding
 */
public class UserRestaurantReviewsAdapter
        extends RecyclerView.Adapter<UserRestaurantReviewsAdapter.ReviewVH> {

    public static class ReviewItem {
        public String reviewId, userName, comment, orderId;
        public float  ratingFood, ratingDelivery, ratingPacking, ratingOverall;
        public long   timestamp;
        public String reviewImageBase64;

        public ReviewItem(String reviewId, String userName, String comment,
                          float ratingFood, float ratingDelivery, float ratingPacking,
                          float ratingOverall, long timestamp, String orderId,
                          String reviewImageBase64) {
            this.reviewId          = reviewId;
            this.userName          = userName;
            this.comment           = comment;
            this.ratingFood        = ratingFood;
            this.ratingDelivery    = ratingDelivery;
            this.ratingPacking     = ratingPacking;
            this.ratingOverall     = ratingOverall;
            this.timestamp         = timestamp;
            this.orderId           = orderId;
            this.reviewImageBase64 = reviewImageBase64;
        }
    }

    private final Context           context;
    private final List<ReviewItem>  list;

    public UserRestaurantReviewsAdapter(Context context, List<ReviewItem> list) {
        this.context = context;
        this.list    = list;
    }

    @NonNull
    @Override
    public ReviewVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_user_review_card, parent, false);
        return new ReviewVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewVH h, int pos) {
        ReviewItem r = list.get(pos);

        // Avatar initial
        String initial = (r.userName != null && !r.userName.isEmpty())
                ? String.valueOf(r.userName.charAt(0)).toUpperCase() : "?";
        h.tvInitial.setText(initial);

        h.tvUserName.setText(r.userName != null ? r.userName : "Anonymous");

        // Overall rating + color
        h.tvOverall.setText(String.format(Locale.getDefault(), "%.1f ⭐", r.ratingOverall));
        if      (r.ratingOverall >= 3.5f) h.tvOverall.setTextColor(0xFF2E7D32);
        else if (r.ratingOverall >= 2.0f) h.tvOverall.setTextColor(0xFFFF8F00);
        else                              h.tvOverall.setTextColor(0xFFE23744);

        // Date
        if (r.timestamp > 0) {
            h.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date(r.timestamp)));
        }

        // Sub-ratings
        h.tvFoodSub.setText("🍽️ Food  " + starsOf(r.ratingFood));
        h.tvDeliverySub.setText("🛵 Delivery  " + starsOf(r.ratingDelivery));
        h.tvPackingSub.setText("📦 Packing  " + starsOf(r.ratingPacking));

        // Comment
        if (r.comment != null && !r.comment.isEmpty()) {
            h.tvComment.setVisibility(View.VISIBLE);
            h.tvComment.setText(r.comment);
        } else {
            h.tvComment.setVisibility(View.GONE);
        }

        // ✅ Review Photo — show only when present, completely hidden otherwise
        if (r.reviewImageBase64 != null && !r.reviewImageBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(r.reviewImageBase64, Base64.DEFAULT);
                Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    if (h.cardReviewPhoto != null) h.cardReviewPhoto.setVisibility(View.VISIBLE);
                    h.ivReviewPhoto.setImageBitmap(bmp);
                    h.ivReviewPhoto.setVisibility(View.VISIBLE);
                } else {
                    if (h.cardReviewPhoto != null) h.cardReviewPhoto.setVisibility(View.GONE);
                    h.ivReviewPhoto.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                if (h.cardReviewPhoto != null) h.cardReviewPhoto.setVisibility(View.GONE);
                h.ivReviewPhoto.setVisibility(View.GONE);
            }
        } else {
            if (h.cardReviewPhoto != null) h.cardReviewPhoto.setVisibility(View.GONE);
            h.ivReviewPhoto.setVisibility(View.GONE);
        }
    }

    private String starsOf(float r) {
        int filled = Math.round(r);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= filled ? "★" : "☆");
        return sb.toString();
    }

    @Override public int getItemCount() { return list == null ? 0 : list.size(); }

    static class ReviewVH extends RecyclerView.ViewHolder {
        TextView  tvInitial, tvUserName, tvOverall, tvDate,
                tvFoodSub, tvDeliverySub, tvPackingSub, tvComment;
        ImageView         ivReviewPhoto;
        MaterialCardView  cardReviewPhoto;

        ReviewVH(@NonNull View v) {
            super(v);
            tvInitial      = v.findViewById(R.id.tvReviewInitial);
            tvUserName     = v.findViewById(R.id.tvReviewerName);
            tvOverall      = v.findViewById(R.id.tvReviewOverall);
            tvDate         = v.findViewById(R.id.tvReviewDateUser);
            tvFoodSub      = v.findViewById(R.id.tvSubFood);
            tvDeliverySub  = v.findViewById(R.id.tvSubDelivery);
            tvPackingSub   = v.findViewById(R.id.tvSubPacking);
            tvComment      = v.findViewById(R.id.tvReviewCommentUser);
            ivReviewPhoto  = v.findViewById(R.id.ivReviewPhotoUser);
            cardReviewPhoto= v.findViewById(R.id.cardUserReviewPhoto);
        }
    }
}