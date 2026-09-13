package com.example.fooddeliveryapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.VH> {

    public interface OnSuggestionClick { void onClick(String query); }

    private final Context context;
    private final List<String> suggestions;
    private final OnSuggestionClick listener;

    public SuggestionAdapter(Context context, List<String> suggestions,
                             OnSuggestionClick listener) {
        this.context     = context;
        this.suggestions = suggestions;
        this.listener    = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context)
                .inflate(R.layout.item_suggestion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String s = suggestions.get(position);
        h.tvSuggestion.setText(s);
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(s); });
    }

    @Override public int getItemCount() { return suggestions.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvSuggestion;
        VH(@NonNull View v) {
            super(v);
            tvSuggestion = v.findViewById(R.id.tvSuggestion);
        }
    }
}