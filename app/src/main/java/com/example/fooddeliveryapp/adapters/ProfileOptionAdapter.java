package com.example.fooddeliveryapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;

import java.util.List;

public class ProfileOptionAdapter extends RecyclerView.Adapter<ProfileOptionAdapter.ViewHolder> {

    private List<ProfileOption> options;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ProfileOption option);
    }

    public ProfileOptionAdapter(List<ProfileOption> options, OnItemClickListener listener) {
        this.options = options;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfileOption option = options.get(position);
        holder.ivIcon.setImageResource(option.getIconResId());
        holder.tvTitle.setText(option.getTitle());
        holder.tvSubtitle.setText(option.getSubtitle());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(option);
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }
}