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
import com.example.fooddeliveryapp.models.FoodItem;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AdminFoodAdapter extends RecyclerView.Adapter<AdminFoodAdapter.FoodViewHolder> {

    public interface AdminFoodActionListener {
        void onEditFood(FoodItem item);
        void onDeleteFood(FoodItem item);
    }

    private final Context context;
    private final List<FoodItem> foodList;
    private final AdminFoodActionListener listener;

    public AdminFoodAdapter(Context context, List<FoodItem> list, AdminFoodActionListener l) {
        this.context  = context;
        this.foodList = list;
        this.listener = l;
    }

    @NonNull @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_admin_food, parent, false);
        return new FoodViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder h, int pos) {
        FoodItem item = foodList.get(pos);

        h.tvName.setText(item.getName() != null ? item.getName() : "");
        h.tvDesc.setText(item.getDescription() != null ? item.getDescription() : "");
        h.tvPrice.setText("₹" + String.format("%.0f", item.getPrice()));

        // Veg badge
        if (item.isVeg()) {
            h.tvVegBadge.setVisibility(View.VISIBLE);
        } else {
            h.tvVegBadge.setVisibility(View.GONE);
        }

        // Offer badge
        String offer = item.getOffer();
        if (offer != null && !offer.isEmpty()) {
            h.tvOfferBadge.setText(offer);
            h.tvOfferBadge.setVisibility(View.VISIBLE);
        } else {
            h.tvOfferBadge.setVisibility(View.GONE);
        }

        // Image
        String b64 = item.getImageBase64();
        if (b64 != null && !b64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) h.ivImage.setImageBitmap(bmp);
                else h.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
            } catch (Exception e) {
                h.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            h.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        h.btnEdit.setOnClickListener(v -> listener.onEditFood(item));
        h.btnDelete.setOnClickListener(v -> listener.onDeleteFood(item));
    }

    @Override public int getItemCount() { return foodList.size(); }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        ImageView     ivImage;
        TextView      tvName, tvDesc, tvPrice, tvVegBadge, tvOfferBadge;
        MaterialButton btnEdit, btnDelete;

        FoodViewHolder(@NonNull View v) {
            super(v);
            ivImage      = v.findViewById(R.id.ivAdminFoodImage);
            tvName       = v.findViewById(R.id.tvAdminFoodName);
            tvDesc       = v.findViewById(R.id.tvAdminFoodDesc);
            tvPrice      = v.findViewById(R.id.tvAdminFoodPrice);
            tvVegBadge   = v.findViewById(R.id.tvVegBadge);
            tvOfferBadge = v.findViewById(R.id.tvOfferBadge);
            btnEdit      = v.findViewById(R.id.btnEditFood);
            btnDelete    = v.findViewById(R.id.btnDeleteFood);
        }
    }
}