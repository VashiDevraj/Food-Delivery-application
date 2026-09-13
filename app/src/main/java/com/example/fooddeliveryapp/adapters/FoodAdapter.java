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
import com.example.fooddeliveryapp.utils.FavouriteManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * FoodAdapter — Swiggy/Zomato style food cards.
 * Compatible with item_food.xml which has:
 *  - Large image at top (150dp)
 *  - ADD + button overlaid on bottom-right of image
 *  - Heart button (top-right) for favourites
 *  - Name + price below image
 */
public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    public interface OnFoodClickListener {
        void onFoodClick(FoodItem foodItem);
        void onAddToCartClick(FoodItem foodItem);
    }

    private final Context             context;
    private final List<FoodItem>      foodList;
    private final OnFoodClickListener listener;
    private final FavouriteManager    favManager;

    public FoodAdapter(Context context, List<FoodItem> foodList, OnFoodClickListener listener) {
        this.context    = context;
        this.foodList   = foodList;
        this.listener   = listener;
        this.favManager = new FavouriteManager(context);
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem food = foodList.get(position);

        holder.tvFoodName.setText(food.getName() != null ? food.getName() : "");
        holder.tvFoodPrice.setText("₹" + (int) food.getPrice());

        // ── Image ──────────────────────────────────────────────────────────────
        loadFoodImage(food, holder.ivFoodImage);

        // ── Heart (favourite) ──────────────────────────────────────────────────
        if (holder.ivFavourite != null) {
            boolean isFav = favManager.isFoodFavourite(food.getFoodId());
            updateHeartIcon(holder.ivFavourite, isFav);

            holder.ivFavourite.setOnClickListener(v -> {
                boolean nowFav = !favManager.isFoodFavourite(food.getFoodId());
                favManager.setFoodFavourite(food, nowFav);
                updateHeartIcon(holder.ivFavourite, nowFav);
                try {
                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.anim_heart_bounce);
                    holder.ivFavourite.startAnimation(anim);
                } catch (Exception ignored) {}
            });
        }

        // ── Click whole card ───────────────────────────────────────────────────
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFoodClick(food);
        });

        // ── ADD to cart button ─────────────────────────────────────────────────
        if (holder.btnAddToCart != null) {
            holder.btnAddToCart.setOnClickListener(v -> {
                if (listener != null) listener.onAddToCartClick(food);
            });
        }
    }

    private void loadFoodImage(FoodItem food, ImageView iv) {
        String img = food.getImageBase64();
        if (img != null && !img.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(img, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    iv.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception ignored) {}
        }
        iv.setImageResource(R.drawable.ic_food_placeholder);
    }

    private void updateHeartIcon(ImageView iv, boolean isFav) {
        if (isFav) {
            iv.setImageResource(R.drawable.ic_favorite_filled);
            iv.setColorFilter(context.getResources().getColor(R.color.primaryColor));
        } else {
            iv.setImageResource(R.drawable.ic_favorite_border);
            iv.clearColorFilter();
        }
    }

    @Override
    public int getItemCount() { return foodList == null ? 0 : foodList.size(); }

    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        ImageView      ivFoodImage;
        ImageView      ivFavourite;
        TextView       tvFoodName;
        TextView       tvFoodPrice;
        MaterialButton btnAddToCart;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoodImage  = itemView.findViewById(R.id.ivFoodImage);
            ivFavourite  = itemView.findViewById(R.id.ivFoodFavourite);
            tvFoodName   = itemView.findViewById(R.id.tvFoodName);
            tvFoodPrice  = itemView.findViewById(R.id.tvFoodPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }
    }
}