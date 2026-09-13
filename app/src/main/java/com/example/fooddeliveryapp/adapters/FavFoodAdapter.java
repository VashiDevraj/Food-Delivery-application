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
import com.example.fooddeliveryapp.models.FoodItem;

import java.util.List;

public class FavFoodAdapter extends RecyclerView.Adapter<FavFoodAdapter.VH> {

    public interface OnClickListener  { void onClick(FoodItem f); }
    public interface OnRemoveListener { void onRemove(FoodItem f); }

    private final Context context;
    private final List<FoodItem> list;
    private final OnClickListener  onClick;
    private final OnRemoveListener onRemove;

    public FavFoodAdapter(Context context, List<FoodItem> list,
                          OnClickListener onClick, OnRemoveListener onRemove) {
        this.context  = context;
        this.list     = list;
        this.onClick  = onClick;
        this.onRemove = onRemove;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context)
                .inflate(R.layout.item_fav_food, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        FoodItem f = list.get(position);
        h.tvName.setText(f.getName() != null ? f.getName() : "");
        h.tvPrice.setText("₹" + (int) f.getPrice());

        // Image
        String img = f.getImageBase64();
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

        // Heart always filled red in favourites screen
        h.ivRemove.setImageResource(R.drawable.ic_favorite_filled);
        h.ivRemove.setColorFilter(0xFFE23744);

        h.itemView.setOnClickListener(v -> { if (onClick != null) onClick.onClick(f); });

        h.ivRemove.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(context, R.anim.anim_heart_bounce);
            h.ivRemove.startAnimation(anim);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation a) {}
                @Override public void onAnimationRepeat(Animation a) {}
                @Override public void onAnimationEnd(Animation a) {
                    if (onRemove != null) onRemove.onRemove(f);
                }
            });
        });
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivImage, ivRemove;
        TextView  tvName, tvPrice;
        VH(@NonNull View v) {
            super(v);
            ivImage  = v.findViewById(R.id.ivFavFoodImage);
            ivRemove = v.findViewById(R.id.ivRemoveFavFood);
            tvName   = v.findViewById(R.id.tvFavFoodName);
            tvPrice  = v.findViewById(R.id.tvFavFoodPrice);
        }
    }
}