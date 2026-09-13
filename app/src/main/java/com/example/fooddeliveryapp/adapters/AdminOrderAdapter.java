package com.example.fooddeliveryapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
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
 * AdminOrderAdapter — shows all orders to admin.
 * ✅ Assign Delivery Boy button visible on every active order card.
 * ✅ Customer delivery notes displayed in yellow banner.
 * ✅ Color-coded payment & status badges.
 * ✅ Long-press also triggers assign (secondary shortcut).
 */
public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder> {

    public interface OnStatusUpdateListener {
        void onStatusUpdate(Order order, String newStatus);
    }

    public interface OnAssignClickListener {
        void onAssignClick(Order order);
    }

    private static final String[] STATUS_OPTIONS = {
            Constants.STATUS_PENDING,
            Constants.STATUS_PREPARING,
            Constants.STATUS_OUT_FOR_DELIVERY,
            Constants.STATUS_DELIVERED,
            Constants.STATUS_CANCELLED
    };

    private static final String[] STATUS_LABELS = {
            "⏳ Pending",
            "🍳 Preparing",
            "🚗 Out for Delivery",
            "✅ Delivered",
            "❌ Cancel Order"
    };

    private final Context                context;
    private final List<Order>            orderList;
    private final OnStatusUpdateListener listener;
    private final OnAssignClickListener  assignListener;
    private int                          lastAnimatedPosition = -1;

    public AdminOrderAdapter(Context context,
                             List<Order> orderList,
                             OnStatusUpdateListener listener,
                             OnAssignClickListener assignListener) {
        this.context        = context;
        this.orderList      = orderList;
        this.listener       = listener;
        this.assignListener = assignListener;
    }

    @NonNull
    @Override
    public AdminOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_order, parent, false);
        return new AdminOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminOrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        bindOrderData(holder, order);
        animateCard(holder.itemView, position);

        // Long-press also triggers assign (bonus shortcut)
        holder.itemView.setOnLongClickListener(v -> {
            if (assignListener != null) {
                assignListener.onAssignClick(order);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void bindOrderData(@NonNull AdminOrderViewHolder holder, Order order) {

        holder.tvOrderId.setText(order.getShortOrderId());

        String name = (order.getUserName() != null && !order.getUserName().isEmpty())
                ? order.getUserName() : "Unknown Customer";
        holder.tvCustomerName.setText(name);

        holder.tvOrderAmount.setText(
                String.format(Locale.getDefault(), "₹%.2f", order.getTotalAmount()));

        SimpleDateFormat sdf =
                new SimpleDateFormat("dd MMM yyyy  •  hh:mm a", Locale.getDefault());
        holder.tvOrderTime.setText(sdf.format(new Date(order.getTimestamp())));

        holder.tvPaymentMethod.setText(order.getPaymentMethod());
        holder.tvPaymentStatus.setText(order.getPaymentStatus());

        boolean isPaid = "Paid".equalsIgnoreCase(order.getPaymentStatus())
                || "Success".equalsIgnoreCase(order.getPaymentStatus());
        holder.tvPaymentStatus.setTextColor(
                ContextCompat.getColor(context,
                        isPaid ? R.color.statusDelivered : R.color.statusPending));

        holder.tvAddress.setText(order.getAddress());
        holder.tvPhone.setText(order.getPhone());
        holder.tvOrderItems.setText(buildItemsSummary(order));

        int count = order.getItemCount();
        holder.tvItemCount.setText(count + (count == 1 ? " item" : " items"));

        // ── Customer Notes ────────────────────────────────────────────────
        String notes = order.getNotes();
        if (notes != null && !notes.isEmpty() && !"null".equals(notes)) {
            holder.layoutCustomerNote.setVisibility(View.VISIBLE);
            holder.tvCustomerNotes.setText(notes);
        } else {
            holder.layoutCustomerNote.setVisibility(View.GONE);
        }

        // ── Delivery boy assignment info ───────────────────────────────────
        boolean isAssigned = order.isAssigned();
        if (isAssigned) {
            holder.tvAssignedBoy.setVisibility(View.VISIBLE);
            holder.tvAssignedBoy.setText("🛵 " + order.getDeliveryBoyName());
        } else {
            holder.tvAssignedBoy.setVisibility(View.GONE);
        }

        // ── Status badge ──────────────────────────────────────────────────
        String status = order.getStatus();
        holder.tvOrderStatus.setText(getStatusLabel(status));
        applyStatusStyle(holder.tvOrderStatus, status);

        // ── Assign button — shown for all non-final orders ────────────────
        boolean isFinal = Constants.STATUS_DELIVERED.equals(status)
                || Constants.STATUS_CANCELLED.equals(status);

        holder.btnAssignDelivery.setVisibility(isFinal ? View.GONE : View.VISIBLE);
        if (!isFinal) {
            holder.btnAssignDelivery.setText(isAssigned ? "Reassign Partner 🔄" : "Assign Partner 🛵");
            holder.btnAssignDelivery.setOnClickListener(v -> {
                if (assignListener != null) assignListener.onAssignClick(order);
            });
        }

        // ── Update-Status button ──────────────────────────────────────────
        holder.btnUpdateStatus.setEnabled(!isFinal);
        holder.btnUpdateStatus.setAlpha(isFinal ? 0.45f : 1.0f);

        if (Constants.STATUS_DELIVERED.equals(status)) {
            holder.btnUpdateStatus.setText("Delivered ✓");
        } else if (Constants.STATUS_CANCELLED.equals(status)) {
            holder.btnUpdateStatus.setText("Cancelled ✗");
        } else {
            holder.btnUpdateStatus.setText("Update Status");
        }

        holder.btnUpdateStatus.setOnClickListener(v -> showStatusDialog(order));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String buildItemsSummary(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) return "No items";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, CartItem> entry : order.getItems().entrySet()) {
            CartItem item = entry.getValue();
            if (item != null) {
                sb.append("• ")
                        .append(item.getName() != null ? item.getName() : "Item")
                        .append("  ×").append(item.getQuantity())
                        .append("  —  ₹")
                        .append(String.format(Locale.getDefault(), "%.2f",
                                item.getPrice() * item.getQuantity()))
                        .append("\n");
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? "No items" : result;
    }

    private String getStatusLabel(String status) {
        if (status == null) return STATUS_LABELS[0];
        switch (status) {
            case Constants.STATUS_PREPARING:        return STATUS_LABELS[1];
            case Constants.STATUS_OUT_FOR_DELIVERY: return STATUS_LABELS[2];
            case Constants.STATUS_DELIVERED:        return STATUS_LABELS[3];
            case Constants.STATUS_CANCELLED:        return STATUS_LABELS[4];
            default:                                return STATUS_LABELS[0];
        }
    }

    private void applyStatusStyle(TextView tv, String status) {
        if (status == null) status = Constants.STATUS_PENDING;
        switch (status) {
            case Constants.STATUS_PREPARING:
                tv.setBackgroundResource(R.drawable.bg_status_preparing); break;
            case Constants.STATUS_OUT_FOR_DELIVERY:
                tv.setBackgroundResource(R.drawable.bg_status_out_for_delivery); break;
            case Constants.STATUS_DELIVERED:
                tv.setBackgroundResource(R.drawable.bg_status_delivered); break;
            case Constants.STATUS_CANCELLED:
                tv.setBackgroundResource(R.drawable.bg_status_cancelled); break;
            default:
                tv.setBackgroundResource(R.drawable.bg_status_pending);   break;
        }
    }

    private void showStatusDialog(Order order) {
        new AlertDialog.Builder(context, R.style.AdminStatusDialogTheme)
                .setTitle("Change Order Status")
                .setIcon(R.drawable.ic_edit_status)
                .setSingleChoiceItems(STATUS_LABELS, getStatusIndex(order.getStatus()), null)
                .setPositiveButton("Apply", (dialog, which) -> {
                    AlertDialog d = (AlertDialog) dialog;
                    int selected = d.getListView().getCheckedItemPosition();
                    if (selected >= 0 && selected < STATUS_OPTIONS.length) {
                        String newStatus = STATUS_OPTIONS[selected];
                        if (Constants.STATUS_CANCELLED.equals(newStatus)) {
                            showCancelReasonDialog(order);
                        } else {
                            if (listener != null) listener.onStatusUpdate(order, newStatus);
                        }
                    }
                })
                .setNegativeButton("Dismiss", null)
                .show();
    }

    private void showCancelReasonDialog(Order order) {
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setHint("Reason for cancellation (optional)");
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(context)
                .setTitle("Cancel Order")
                .setMessage("Please provide a reason:")
                .setView(input)
                .setPositiveButton("Confirm Cancel", (d, w) -> {
                    String reason = input.getText().toString().trim();
                    order.setCancelReason(reason);
                    order.setCancelledBy("admin");
                    if (listener != null) listener.onStatusUpdate(order, Constants.STATUS_CANCELLED);
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private int getStatusIndex(String status) {
        if (status == null) return 0;
        switch (status) {
            case Constants.STATUS_PREPARING:        return 1;
            case Constants.STATUS_OUT_FOR_DELIVERY: return 2;
            case Constants.STATUS_DELIVERED:        return 3;
            case Constants.STATUS_CANCELLED:        return 4;
            default:                                return 0;
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

    // ─────────────────────────────────────────────────────────────────────────

    public static class AdminOrderViewHolder extends RecyclerView.ViewHolder {
        final TextView       tvOrderId;
        final TextView       tvCustomerName;
        final TextView       tvOrderAmount;
        final TextView       tvOrderTime;
        final TextView       tvOrderItems;
        final TextView       tvOrderStatus;
        final TextView       tvPaymentMethod;
        final TextView       tvPaymentStatus;
        final TextView       tvAddress;
        final TextView       tvPhone;
        final TextView       tvItemCount;
        final TextView       tvCustomerNotes;
        final TextView       tvAssignedBoy;
        final LinearLayout   layoutCustomerNote;
        final MaterialButton btnUpdateStatus;
        final MaterialButton btnAssignDelivery;

        public AdminOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId          = itemView.findViewById(R.id.tvAdminOrderId);
            tvCustomerName     = itemView.findViewById(R.id.tvCustomerName);
            tvOrderAmount      = itemView.findViewById(R.id.tvAdminOrderAmount);
            tvOrderTime        = itemView.findViewById(R.id.tvAdminOrderTime);
            tvOrderItems       = itemView.findViewById(R.id.tvAdminOrderItems);
            tvOrderStatus      = itemView.findViewById(R.id.tvAdminOrderStatus);
            tvPaymentMethod    = itemView.findViewById(R.id.tvPaymentMethod);
            tvPaymentStatus    = itemView.findViewById(R.id.tvPaymentStatus);
            tvAddress          = itemView.findViewById(R.id.tvAddress);
            tvPhone            = itemView.findViewById(R.id.tvPhone);
            tvItemCount        = itemView.findViewById(R.id.tvItemCount);
            tvCustomerNotes    = itemView.findViewById(R.id.tvCustomerNotes);
            tvAssignedBoy      = itemView.findViewById(R.id.tvAssignedBoy);
            layoutCustomerNote = itemView.findViewById(R.id.layoutCustomerNote);
            btnUpdateStatus    = itemView.findViewById(R.id.btnUpdateStatus);
            btnAssignDelivery  = itemView.findViewById(R.id.btnAssignDelivery);
        }
    }
}