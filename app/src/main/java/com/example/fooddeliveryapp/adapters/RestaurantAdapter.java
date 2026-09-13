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
import com.example.fooddeliveryapp.models.Restaurant;
import com.example.fooddeliveryapp.utils.RestaurantStatusHelper;

import java.util.List;

/**
 * RestaurantAdapter
 *
 * Updated to show:
 *  • 🟢 OPEN / 🔴 CLOSED badge on each card
 *  • Opening time when closed ("Opens at 9:00 AM")
 *  • Semi-transparent overlay on closed restaurant cards
 *  • Offer banner if present
 */
public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.VH> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    private final Context                   context;
    private final List<Restaurant>          list;
    private final OnRestaurantClickListener listener;

    public RestaurantAdapter(Context context, List<Restaurant> list,
                             OnRestaurantClickListener listener) {
        this.context  = context;
        this.list     = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_restaurant_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Restaurant r = list.get(position);
        holder.bind(r);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRestaurantClick(r);
        });
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class VH extends RecyclerView.ViewHolder {

        final ImageView ivRestaurant;
        final TextView  tvName;
        final TextView  tvCity;
        final TextView  tvRating;
        final TextView  tvDelivery;
        final TextView  tvOffer;
        final TextView  tvVegBadge;
        final TextView  tvOpenStatus;      // NEW: 🟢 OPEN / 🔴 CLOSED
        final TextView  tvOpenTime;        // NEW: "Opens at 9:00 AM"
        final View      viewClosedOverlay; // NEW: semi-transparent dim layer

        VH(@NonNull View itemView) {
            super(itemView);
            ivRestaurant     = itemView.findViewById(R.id.ivRestaurantImage);
            tvName           = itemView.findViewById(R.id.tvRestaurantName);
            tvCity           = itemView.findViewById(R.id.tvRestaurantCity);
            tvRating         = itemView.findViewById(R.id.tvRating);
            tvDelivery       = itemView.findViewById(R.id.tvDeliveryTime);
            tvOffer          = itemView.findViewById(R.id.tvOffer);
            tvVegBadge       = itemView.findViewById(R.id.tvVegBadge);
            tvOpenStatus     = itemView.findViewById(R.id.tvOpenStatus);
            tvOpenTime       = itemView.findViewById(R.id.tvOpenTime);
            viewClosedOverlay= itemView.findViewById(R.id.viewClosedOverlay);
        }

        void bind(Restaurant r) {
            tvName.setText(r.getName());
            tvCity.setText(r.getCity());
            tvRating.setText(r.getRating() > 0 ? "★ " + String.format("%.1f", r.getRating()) : "New");
            tvDelivery.setText(r.getDeliveryTime().isEmpty() ? "30–45 min" : r.getDeliveryTime());

            // Offer tag
            if (r.getOffer() != null && !r.getOffer().isEmpty()) {
                tvOffer.setVisibility(View.VISIBLE);
                tvOffer.setText(r.getOffer());
            } else {
                tvOffer.setVisibility(View.GONE);
            }

            // Veg badge
            if (r.isVegOnly()) {
                tvVegBadge.setVisibility(View.VISIBLE);
                tvVegBadge.setText("🌿 Pure Veg");
            } else {
                tvVegBadge.setVisibility(View.GONE);
            }

            // ── Open / Closed status ──────────────────────────────────────────
            boolean isOpen = RestaurantStatusHelper.isOpen(r);
            if (isOpen) {
                if (tvOpenStatus != null) {
                    tvOpenStatus.setVisibility(View.VISIBLE);
                    tvOpenStatus.setText("🟢 Open");
                    tvOpenStatus.setTextColor(0xFF2E7D32);
                    tvOpenStatus.setBackgroundResource(R.drawable.bg_open_pill_green);
                }
                if (tvOpenTime  != null) tvOpenTime.setVisibility(View.GONE);
                if (viewClosedOverlay != null) viewClosedOverlay.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);
            } else {
                if (tvOpenStatus != null) {
                    tvOpenStatus.setVisibility(View.VISIBLE);
                    tvOpenStatus.setText("🔴 Closed");
                    tvOpenStatus.setTextColor(0xFFB71C1C);
                    tvOpenStatus.setBackgroundResource(R.drawable.bg_closed_pill_red);
                }
                if (tvOpenTime != null) {
                    tvOpenTime.setVisibility(View.VISIBLE);
                    tvOpenTime.setText(r.getOpensAtText());
                }
                if (viewClosedOverlay != null) viewClosedOverlay.setVisibility(View.VISIBLE);
                itemView.setAlpha(0.75f);
            }

            // Image
            if (r.getImageBase64() != null && !r.getImageBase64().isEmpty()
                    && !r.getImageBase64().startsWith("http")) {
                try {
                    byte[] bytes = Base64.decode(r.getImageBase64(), Base64.DEFAULT);
                    Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ivRestaurant.setImageBitmap(bmp);
                } catch (Exception e) {
                    ivRestaurant.setImageResource(R.drawable.ic_restaurant_placeholder);
                }
            } else if (r.getImageBase64() != null && r.getImageBase64().startsWith("http")) {
                // URL — load with Glide/Picasso if available; fallback to placeholder
                ivRestaurant.setImageResource(R.drawable.ic_restaurant_placeholder);
            } else {
                ivRestaurant.setImageResource(R.drawable.ic_restaurant_placeholder);
            }
        }
    }
}