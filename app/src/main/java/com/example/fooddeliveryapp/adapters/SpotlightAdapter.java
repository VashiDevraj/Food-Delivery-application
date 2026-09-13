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

import java.util.List;

/**
 * SpotlightAdapter — horizontal scroll cards for "In the Spotlight" section.
 * Layout: item_spotlight_restaurant.xml
 *
 * ✅ FIX: Rating now formatted to exactly 1 decimal place (e.g. 4.0, 4.5)
 *         instead of raw float which could show 4.333333 etc.
 */
public class SpotlightAdapter extends RecyclerView.Adapter<SpotlightAdapter.SpotlightVH> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    private final Context context;
    private final List<Restaurant> list;
    private final OnRestaurantClickListener listener;

    public SpotlightAdapter(Context context, List<Restaurant> list, OnRestaurantClickListener listener) {
        this.context  = context;
        this.list     = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SpotlightVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_spotlight_restaurant, parent, false);
        return new SpotlightVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SpotlightVH holder, int position) {
        Restaurant r = list.get(position);

        holder.tvName.setText(r.getName() != null ? r.getName() : "");

        // ✅ FIX: Format rating to exactly 1 decimal place (e.g. 4.0, 3.7)
        if (r.getRating() > 0) {
            holder.tvRating.setText(String.format("%.1f", r.getRating()));
        } else {
            holder.tvRating.setText("New");
        }

        // Offer badge
        if (r.getOffer() != null && !r.getOffer().isEmpty()) {
            holder.tvOfferBadge.setVisibility(View.VISIBLE);
            holder.tvOfferBadge.setText(r.getOffer());
        } else {
            holder.tvOfferBadge.setVisibility(View.GONE);
        }

        // Restaurant image (Base64)
        if (r.getImageBase64() != null && !r.getImageBase64().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(r.getImageBase64(), Base64.DEFAULT);
                Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) holder.ivRestaurant.setImageBitmap(bmp);
                else             holder.ivRestaurant.setImageResource(R.drawable.ic_restaurant_placeholder);
            } catch (Exception e) {
                holder.ivRestaurant.setImageResource(R.drawable.ic_restaurant_placeholder);
            }
        } else {
            holder.ivRestaurant.setImageResource(R.drawable.ic_restaurant_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRestaurantClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class SpotlightVH extends RecyclerView.ViewHolder {
        ImageView ivRestaurant;
        TextView  tvName, tvRating, tvOfferBadge;

        SpotlightVH(@NonNull View itemView) {
            super(itemView);
            ivRestaurant = itemView.findViewById(R.id.ivSpotlightImage);
            tvName       = itemView.findViewById(R.id.tvSpotlightName);
            tvRating     = itemView.findViewById(R.id.tvSpotlightRating);
            tvOfferBadge = itemView.findViewById(R.id.tvSpotlightOffer);
        }
    }
}