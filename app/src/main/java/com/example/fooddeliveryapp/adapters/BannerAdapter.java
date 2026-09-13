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

import java.util.List;

/**
 * BannerAdapter — ViewPager2 adapter for auto-scrolling offer banners.
 * Accepts Base64 image strings OR URLs from Firebase.
 * Layout: item_banner.xml
 */
public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private final Context context;
    private final List<String> bannerList; // Base64 or URL strings

    public BannerAdapter(Context context, List<String> bannerList) {
        this.context    = context;
        this.bannerList = bannerList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        String data = bannerList.get(position);

        if (data == null || data.isEmpty()) return;

        // Try Base64 decode first
        try {
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) {
                holder.ivBanner.setImageBitmap(bmp);
                return;
            }
        } catch (Exception ignored) {}

        // Fallback: show placeholder with offer text
        holder.ivBanner.setImageResource(R.drawable.bg_offer_banner_placeholder);
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBanner;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.ivBanner);
        }
    }
}