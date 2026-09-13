package com.example.fooddeliveryapp.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.Coupon;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * CouponAdapter — renders coupon cards with:
 *   • Applied state (greyed out, "Applied ✓" button)
 *   • Free-delivery badge for FREEDEL
 *   • Conditions (min order, first order only)
 *
 * Constructor takes two already-applied codes; matching coupons are shown
 * as disabled so the user can't apply them twice.
 */
public class CouponAdapter extends RecyclerView.Adapter<CouponAdapter.CouponViewHolder> {

    public interface OnCouponClickListener {
        void onCouponClick(Coupon coupon);
    }

    private final List<Coupon>          couponList;
    private final String                appliedCode1;
    private final String                appliedCode2;
    private final OnCouponClickListener listener;

    // Legacy constructor (no applied codes) — kept for compatibility
    public CouponAdapter(List<Coupon> couponList, OnCouponClickListener listener) {
        this(couponList, "", "", listener);
    }

    public CouponAdapter(List<Coupon> couponList,
                         String appliedCode1,
                         String appliedCode2,
                         OnCouponClickListener listener) {
        this.couponList   = couponList;
        this.appliedCode1 = appliedCode1 != null ? appliedCode1 : "";
        this.appliedCode2 = appliedCode2 != null ? appliedCode2 : "";
        this.listener     = listener;
    }

    @NonNull
    @Override
    public CouponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_coupon, parent, false);
        return new CouponViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CouponViewHolder holder, int position) {
        Coupon  coupon  = couponList.get(position);
        Context context = holder.itemView.getContext();
        String  code    = coupon.getCode();

        boolean isApplied = code.equals(appliedCode1) || code.equals(appliedCode2);
        boolean isFreeDel = "FREEDEL".equals(code);

        // ── Code & description ────────────────────────────────────────────
        holder.tvCode.setText(code);
        holder.tvDescription.setText(coupon.getDescription());

        // ── Discount badge ────────────────────────────────────────────────
        if (isFreeDel) {
            holder.tvDiscount.setText("FREE\nDELIVERY");
        } else {
            holder.tvDiscount.setText("₹" + coupon.getDiscount() + "\nOFF");
        }

        // ── Condition tag line ────────────────────────────────────────────
        StringBuilder conditions = new StringBuilder();
        if (coupon.getMinOrderValue() > 0) {
            conditions.append("Min. order ₹")
                    .append(String.format("%.0f", coupon.getMinOrderValue()));
        }
        if (coupon.isNewUserOnly()) {
            if (conditions.length() > 0) conditions.append("  •  ");
            conditions.append("First order only");
        }
        if (isFreeDel) {
            if (conditions.length() > 0) conditions.append("  •  ");
            conditions.append("Removes ₹40 delivery fee");
        }

        if (conditions.length() > 0) {
            holder.tvConditions.setVisibility(View.VISIBLE);
            holder.tvConditions.setText(conditions.toString());
        } else {
            holder.tvConditions.setVisibility(View.GONE);
        }

        // ── Applied / disabled state ──────────────────────────────────────
        if (isApplied) {
            holder.itemView.setAlpha(0.6f);
            holder.btnApply.setText("Applied ✓");
            holder.btnApply.setEnabled(false);
            holder.btnApply.setAlpha(0.5f);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.btnApply.setText("Apply");
            holder.btnApply.setEnabled(true);
            holder.btnApply.setAlpha(1.0f);

            holder.btnApply.setOnClickListener(v -> {
                if (listener != null) listener.onCouponClick(coupon);
            });
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCouponClick(coupon);
            });
        }
    }

    @Override
    public int getItemCount() {
        return couponList != null ? couponList.size() : 0;
    }

    static class CouponViewHolder extends RecyclerView.ViewHolder {
        final TextView       tvCode;
        final TextView       tvDescription;
        final TextView       tvConditions;
        final TextView       tvDiscount;
        final MaterialButton btnApply;

        CouponViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode        = itemView.findViewById(R.id.tvCouponCode);
            tvDescription = itemView.findViewById(R.id.tvCouponDesc);
            tvConditions  = itemView.findViewById(R.id.tvCouponConditions);
            tvDiscount    = itemView.findViewById(R.id.tvCouponDiscount);
            btnApply      = itemView.findViewById(R.id.btnApplyCoupon);
        }
    }
}