package com.example.fooddeliveryapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.Restaurant;

import java.util.List;

public class FavRestaurantAdapter extends RecyclerView.Adapter<FavRestaurantAdapter.VH> {

    public interface OnClickListener   { void onClick(Restaurant r); }
    public interface OnRemoveListener  { void onRemove(Restaurant r); }

    private final Context context;
    private final List<Restaurant> list;
    private final OnClickListener  onClick;
    private final OnRemoveListener onRemove;

    public FavRestaurantAdapter(Context context, List<Restaurant> list,
                                OnClickListener onClick, OnRemoveListener onRemove) {
        this.context  = context;
        this.list     = list;
        this.onClick  = onClick;
        this.onRemove = onRemove;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context)
                .inflate(R.layout.item_fav_restaurant, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Restaurant r = list.get(position);
        h.tvName.setText(r.getName() != null ? r.getName() : "");
        h.tvRating.setText(r.getRating() > 0 ? String.format("★ %.1f", r.getRating()) : "");

        // Image
        String img = r.getImageBase64();
        if (img != null && !img.isEmpty()) {
            try {
                byte[] b = Base64.decode(img, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
                if (bmp != null) h.ivImage.setImageBitmap(bmp);
                else h.ivImage.setImageResource(R.drawable.ic_food_placeholder);
            } catch (Exception e) {
                h.ivImage.setImageResource(R.drawable.ic_food_placeholder);
            }
        } else {
            h.ivImage.setImageResource(R.drawable.ic_food_placeholder);
        }

        // Heart is always filled red in favourites screen
        h.ivRemove.setImageResource(R.drawable.ic_favorite_filled);
        h.ivRemove.setColorFilter(0xFFE23744);

        h.itemView.setOnClickListener(v -> { if (onClick != null) onClick.onClick(r); });

        h.ivRemove.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(context, R.anim.anim_heart_bounce);
            h.ivRemove.startAnimation(anim);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}
                @Override public void onAnimationEnd(Animation a) {
                    if (onRemove != null) onRemove.onRemove(r);
                }
            });
        });
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivImage, ivRemove;
        TextView  tvName, tvRating;
        VH(@NonNull View v) {
            super(v);
            ivImage  = v.findViewById(R.id.ivFavRestImage);
            ivRemove = v.findViewById(R.id.ivRemoveFav);
            tvName   = v.findViewById(R.id.tvFavRestName);
            tvRating = v.findViewById(R.id.tvFavRestRating);
        }
    }
}