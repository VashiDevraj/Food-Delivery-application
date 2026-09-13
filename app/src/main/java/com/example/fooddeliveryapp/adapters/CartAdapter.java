package com.example.fooddeliveryapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.models.CartItem;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface CartActionListener {
        void onIncreaseQuantity(CartItem cartItem, int position);
        void onDecreaseQuantity(CartItem cartItem, int position);
        void onRemoveItem(CartItem cartItem, int position);
    }

    private Context context;
    private List<CartItem> cartItemList;
    private CartActionListener listener;

    public CartAdapter(Context context, List<CartItem> cartItemList, CartActionListener listener) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_cart, parent, false);

        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {

        CartItem cartItem = cartItemList.get(position);

        holder.tvCartItemName.setText(cartItem.getName());
        holder.tvCartItemPrice.setText("₹" + cartItem.getPrice());
        holder.tvQuantity.setText(String.valueOf(cartItem.getQuantity()));
        holder.tvCartItemTotal.setText("Total ₹" + cartItem.getTotalPrice());

        holder.btnIncrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIncreaseQuantity(cartItem, position);
            }
        });

        holder.btnDecrease.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecreaseQuantity(cartItem, position);
            }
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveItem(cartItem, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList == null ? 0 : cartItemList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {

        TextView tvCartItemName;
        TextView tvCartItemPrice;
        TextView tvCartItemTotal;
        TextView tvQuantity;

        MaterialButton btnIncrease;
        MaterialButton btnDecrease;
        MaterialButton btnRemove;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCartItemName = itemView.findViewById(R.id.tvCartItemName);
            tvCartItemPrice = itemView.findViewById(R.id.tvCartItemPrice);
            tvCartItemTotal = itemView.findViewById(R.id.tvCartItemTotal);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);

            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}