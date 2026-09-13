package com.example.fooddeliveryapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.activities.OrderTrackingActivity;
import com.example.fooddeliveryapp.models.CartItem;
import com.example.fooddeliveryapp.models.Order;
import com.example.fooddeliveryapp.utils.Constants;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OrderAdapter — Premium RecyclerView adapter for user-facing Order History
 * Bright white theme with orange accents
 * Features: animated cards, color-coded status badge, 3-step progress tracker
 *
 * Status colors:
 *   Pending   → Orange/Amber  (#E65100)
 *   Preparing → Blue          (#1565C0)
 *   Delivered → Green         (#2E7D32)
 */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final Context     context;
    private final List<Order> orderList;
    private int lastAnimatedPosition = -1;
    MaterialButton btnCancelOrder;

    public OrderAdapter(Context context, List<Order> orderList) {
        this.context   = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        bindOrderData(holder, order);
        animateCard(holder.itemView, position);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderTrackingActivity.class);
            intent.putExtra("orderId", order.getOrderId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    private void bindOrderData(@NonNull OrderViewHolder holder, Order order) {

        // Order ID
        holder.tvOrderId.setText(order.getShortOrderId());

        // Status
        String status = order.getStatus();
        holder.tvOrderStatus.setText(getStatusEmoji(status) + "  " + status);
        applyStatusStyle(holder.tvOrderStatus, status);

        // Progress tracker
        updateProgressTracker(holder, status);

        // Amount
        holder.tvOrderAmount.setText(
                String.format(Locale.getDefault(), "₹%.2f", order.getTotalAmount()));

        // Timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy  •  hh:mm a", Locale.getDefault());
        holder.tvOrderTime.setText(sdf.format(new Date(order.getTimestamp())));

        // Items
        holder.tvOrderItems.setText(buildItemsSummary(order));
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderTrackingActivity.class);
            intent.putExtra("orderId", order.getOrderId());
            context.startActivity(intent);
        });
    }

    private String getStatusEmoji(String status) {
        if (status == null) return "⏳";
        switch (status) {
            case Constants.STATUS_PREPARING: return "🍳";
            case Constants.STATUS_DELIVERED: return "✅";
            case Constants.STATUS_CANCELLED:
                return "❌ Cancelled";
            default:                         return "⏳";
        }
    }

    /**
     * Update progress dots and connector colors based on order status
     * Active = orange, Done = green, Inactive = grey
     */
    private void updateProgressTracker(@NonNull OrderViewHolder holder, String status) {
        int orange   = ContextCompat.getColor(context, R.color.primaryOrange);
        int grey     = ContextCompat.getColor(context, R.color.stepInactiveLight);
        int green    = ContextCompat.getColor(context, R.color.statusDelivered);

        boolean isPreparing = Constants.STATUS_PREPARING.equals(status);
        boolean isDelivered = Constants.STATUS_DELIVERED.equals(status);

        // Step 1 — always active (order was placed)
        holder.stepPlaced.setBackgroundTintList(ColorStateList.valueOf(orange));

        // Step 2 — active when Preparing or Delivered
        holder.stepPreparing.setBackgroundTintList(
                ColorStateList.valueOf((isPreparing || isDelivered) ? orange : grey));

        // Step 3 — active (green) only when Delivered
        holder.stepDelivered.setBackgroundTintList(
                ColorStateList.valueOf(isDelivered ? green : grey));

        // Connector lines
        holder.lineOneTwoConnector.setBackgroundColor(
                (isPreparing || isDelivered) ? orange : grey);
        holder.lineTwoThreeConnector.setBackgroundColor(
                isDelivered ? green : grey);
    }

    private String buildItemsSummary(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "No items";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, CartItem> entry : order.getItems().entrySet()) {
            CartItem item = entry.getValue();
            if (item != null) {
                sb.append("• ")
                        .append(item.getName() != null ? item.getName() : "Item")
                        .append("  ×")
                        .append(item.getQuantity())
                        .append("  —  ₹")
                        .append(String.format(Locale.getDefault(), "%.2f",
                                item.getPrice() * item.getQuantity()))
                        .append("\n");
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? "No items" : result;
    }

    private void applyStatusStyle(TextView tv, String status) {
        if (status == null) status = Constants.STATUS_PENDING;
        switch (status) {
            case Constants.STATUS_PREPARING:
                tv.setBackgroundResource(R.drawable.bg_status_preparing);
                break;
            case Constants.STATUS_DELIVERED:
                tv.setBackgroundResource(R.drawable.bg_status_delivered);
                break;
            default:
                tv.setBackgroundResource(R.drawable.bg_status_pending);
                break;
        }
    }

    private void animateCard(View view, int position) {
        if (position > lastAnimatedPosition) {
            view.setAlpha(0f);
            view.setTranslationY(60f);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(320)
                    .setStartDelay(position * 50L)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            lastAnimatedPosition = position;
        }
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {

        final TextView tvOrderId;
        final TextView tvOrderStatus;
        final TextView tvOrderAmount;
        final TextView tvOrderTime;
        final TextView tvOrderItems;

        // Progress tracker
        final View stepPlaced;
        final View stepPreparing;
        final View stepDelivered;
        final View lineOneTwoConnector;
        final View lineTwoThreeConnector;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId    = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderAmount = itemView.findViewById(R.id.tvOrderAmount);
            tvOrderTime   = itemView.findViewById(R.id.tvOrderTime);
            tvOrderItems  = itemView.findViewById(R.id.tvOrderItems);

            stepPlaced            = itemView.findViewById(R.id.stepPlaced);
            stepPreparing         = itemView.findViewById(R.id.stepPreparing);
            stepDelivered         = itemView.findViewById(R.id.stepDelivered);
            lineOneTwoConnector   = itemView.findViewById(R.id.lineOneTwoConnector);
            lineTwoThreeConnector = itemView.findViewById(R.id.lineTwoThreeConnector);
        }
    }
}