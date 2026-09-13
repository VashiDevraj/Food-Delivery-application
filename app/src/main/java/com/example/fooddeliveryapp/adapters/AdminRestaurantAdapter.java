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
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AdminRestaurantAdapter
        extends RecyclerView.Adapter<AdminRestaurantAdapter.RestaurantVH> {

    public interface OnRestaurantActionListener {
        void onEdit(Restaurant r);
        void onDelete(Restaurant r);
    }

    private final Context context;
    private final List<Restaurant> list;
    private final OnRestaurantActionListener listener;

    public AdminRestaurantAdapter(Context ctx, List<Restaurant> list,
                                  OnRestaurantActionListener l) {
        this.context  = ctx;
        this.list     = list;
        this.listener = l;
    }

    @NonNull @Override
    public RestaurantVH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_restaurant, parent, false);
        return new RestaurantVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantVH h, int pos) {
        Restaurant r = list.get(pos);

        h.tvName.setText(r.getName() != null ? r.getName() : "");
        h.tvCity.setText("📍 " + (r.getCity() != null ? r.getCity() : ""));

        // Veg only badge
        if (r.isVegOnly()) {
            h.tvVegBadge.setVisibility(View.VISIBLE);
        } else {
            h.tvVegBadge.setVisibility(View.GONE);
        }

        // Offer
        String offer = r.getOffer();
        if (offer != null && !offer.isEmpty()) {
            h.tvOffer.setText(offer);
            h.tvOffer.setVisibility(View.VISIBLE);
        } else {
            h.tvOffer.setVisibility(View.GONE);
        }

        // Image
        String b64 = r.getImageBase64();
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

        h.btnEdit.setOnClickListener(v -> listener.onEdit(r));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(r));
    }

    @Override public int getItemCount() { return list.size(); }

    static class RestaurantVH extends RecyclerView.ViewHolder {
        ImageView      ivImage;
        TextView       tvName, tvCity, tvOffer, tvVegBadge;
        MaterialButton btnEdit, btnDelete;

        RestaurantVH(@NonNull View v) {
            super(v);
            ivImage    = v.findViewById(R.id.ivRestaurantImage);
            tvName     = v.findViewById(R.id.tvRestaurantName);
            tvCity     = v.findViewById(R.id.tvRestaurantCity);
            tvOffer    = v.findViewById(R.id.tvRestaurantOffer);
            tvVegBadge = v.findViewById(R.id.tvVegOnlyBadge);
            btnEdit    = v.findViewById(R.id.btnEditRestaurant);
            btnDelete  = v.findViewById(R.id.btnDeleteRestaurant);
        }
    }
}