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
import com.example.fooddeliveryapp.models.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private Context context;
    private List<Category> categoryList;
    private OnCategoryClickListener listener;

    public CategoryAdapter(Context context, List<Category> categoryList,
                           OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_category, parent, false);

        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {

        Category category = categoryList.get(position);

        holder.tvCategoryName.setText(category.getCategoryName());

        if (category.getCategoryImage() != null && !category.getCategoryImage().isEmpty()) {

            try {
                byte[] bytes = Base64.decode(category.getCategoryImage(), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                if (bmp != null) {
                    holder.ivCategory.setImageBitmap(bmp);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            holder.ivCategory.setImageResource(R.drawable.ic_category_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public void updateList(List<Category> newList) {
        this.categoryList = newList;
        notifyDataSetChanged();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {

        ImageView ivCategory;
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            ivCategory = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}