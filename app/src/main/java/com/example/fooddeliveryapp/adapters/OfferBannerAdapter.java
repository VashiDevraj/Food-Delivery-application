package com.example.fooddeliveryapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.OfferBanner;

import java.util.List;

/**
 * OfferBannerAdapter — drives the ViewPager2 banner carousel on the user dashboard.
 *
 * Each card shows:
 *  • Gradient background (bgColorStart → bgColorEnd)
 *  • Badge label (e.g. "LIMITED TIME")
 *  • Big title  (e.g. "50% OFF")
 *  • Subtitle   (e.g. "on your first order above ₹199")
 *  • CTA button text
 *
 * The adapter also supports a click listener so the host Activity can navigate
 * to a linked restaurant or apply a filter.
 */
public class OfferBannerAdapter extends RecyclerView.Adapter<OfferBannerAdapter.BannerVH> {

    public interface OnBannerClickListener {
        void onBannerClick(OfferBanner banner);
    }

    private final Context              context;
    private final List<OfferBanner>    banners;
    private final OnBannerClickListener clickListener;

    // Fallback gradient palettes used when Firebase banners aren't loaded yet
    private static final int[][] FALLBACK_PALETTES = {
            {0xFFE23744, 0xFFC62828},   // red
            {0xFF1565C0, 0xFF0D47A1},   // blue
            {0xFF2E7D32, 0xFF1B5E20},   // green
            {0xFFE65100, 0xFFBF360C},   // deep-orange
            {0xFF4A148C, 0xFF38006B},   // purple
    };

    private static final String[] FALLBACK_BADGES    = {"🔥 HOT DEAL", "✨ EXCLUSIVE", "🎉 SPECIAL", "⚡ FLASH SALE", "💎 PREMIUM"};
    private static final String[] FALLBACK_TITLES    = {"50% OFF", "Free Delivery", "₹100 Cashback", "Buy 1 Get 1", "Flat ₹60 OFF"};
    private static final String[] FALLBACK_SUBTITLES = {
            "on select items · limited time",
            "on orders above ₹199",
            "use code CASH100",
            "on weekends only",
            "above ₹249 · tonight only"
    };

    public OfferBannerAdapter(Context context, List<OfferBanner> banners,
                              OnBannerClickListener clickListener) {
        this.context       = context;
        this.banners       = banners;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public BannerVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_offer_banner, parent, false);
        return new BannerVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerVH holder, int position) {
        if (banners != null && !banners.isEmpty() && position < banners.size()) {
            OfferBanner banner = banners.get(position);
            holder.bind(banner, position);
        } else {
            holder.bindFallback(position);
        }
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null && banners != null && !banners.isEmpty()
                    && position < banners.size()) {
                clickListener.onBannerClick(banners.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return (banners != null && !banners.isEmpty()) ? banners.size() : FALLBACK_TITLES.length;
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class BannerVH extends RecyclerView.ViewHolder {

        final View     cardRoot;
        final TextView tvBadge;
        final TextView tvTitle;
        final TextView tvSubtitle;
        final TextView tvCta;
        final TextView tvEmoji;

        BannerVH(@NonNull View itemView) {
            super(itemView);
            cardRoot  = itemView.findViewById(R.id.bannerCardRoot);
            tvBadge   = itemView.findViewById(R.id.tvBannerBadge);
            tvTitle   = itemView.findViewById(R.id.tvBannerTitle);
            tvSubtitle= itemView.findViewById(R.id.tvBannerSubtitle);
            tvCta     = itemView.findViewById(R.id.tvBannerCta);
            tvEmoji   = itemView.findViewById(R.id.tvBannerEmoji);
        }

        void bind(OfferBanner b, int position) {
            tvBadge.setText(b.getBadgeText().isEmpty()
                    ? FALLBACK_BADGES[position % FALLBACK_BADGES.length] : b.getBadgeText());
            tvTitle.setText(b.getTitle().isEmpty()
                    ? FALLBACK_TITLES[position % FALLBACK_TITLES.length] : b.getTitle());
            tvSubtitle.setText(b.getSubtitle().isEmpty()
                    ? FALLBACK_SUBTITLES[position % FALLBACK_SUBTITLES.length] : b.getSubtitle());
            tvCta.setText(b.getCtaText().isEmpty() ? "Order Now →" : b.getCtaText() + " →");

            applyGradient(b.getBgColorStart(), b.getBgColorEnd(), position);

            String[] emojis = {"🍕","🍔","🍜","🥗","🍣","🍦","🌮","🥘"};
            tvEmoji.setText(emojis[position % emojis.length]);
        }

        void bindFallback(int position) {
            int idx = position % FALLBACK_TITLES.length;
            tvBadge.setText(FALLBACK_BADGES[idx]);
            tvTitle.setText(FALLBACK_TITLES[idx]);
            tvSubtitle.setText(FALLBACK_SUBTITLES[idx]);
            tvCta.setText("Order Now →");
            tvEmoji.setText(new String[]{"🍕","🍔","🍜","🥗","🍦"}[idx]);

            int[] cols = FALLBACK_PALETTES[idx];
            applyGradientInt(cols[0], cols[1]);
        }

        private void applyGradient(String start, String end, int position) {
            try {
                int s = Color.parseColor(start);
                int e = Color.parseColor(end);
                applyGradientInt(s, e);
            } catch (Exception ex) {
                int[] cols = FALLBACK_PALETTES[position % FALLBACK_PALETTES.length];
                applyGradientInt(cols[0], cols[1]);
            }
        }

        private void applyGradientInt(int start, int end) {
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{start, end});
            gd.setCornerRadius(dpToPx(20));
            cardRoot.setBackground(gd);
        }

        private float dpToPx(float dp) {
            return dp * itemView.getContext().getResources()
                    .getDisplayMetrics().density;
        }

        private static final String[] FALLBACK_BADGES    = {"🔥 HOT DEAL", "✨ EXCLUSIVE", "🎉 SPECIAL", "⚡ FLASH SALE", "💎 PREMIUM"};
        private static final String[] FALLBACK_TITLES    = {"50% OFF", "Free Delivery", "₹100 Cashback", "Buy 1 Get 1", "Flat ₹60 OFF"};
        private static final String[] FALLBACK_SUBTITLES = {
                "on select items · limited time",
                "on orders above ₹199",
                "use code CASH100",
                "on weekends only",
                "above ₹249 · tonight only"
        };
    }
}