package com.example.fooddeliveryapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.activities.AddressListActivity;
import com.example.fooddeliveryapp.models.Address;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.VH> {

    Context context;
    List<Address> list;

    public AddressAdapter(Context context, List<Address> list) {
        this.context = context;
        this.list = list;
    }

    class VH extends RecyclerView.ViewHolder {
        RadioButton rb;
        MaterialButton edit, delete;

        public VH(View v) {
            super(v);
            rb = v.findViewById(R.id.rbAddress);
            edit = v.findViewById(R.id.btnEditAddress);
            delete = v.findViewById(R.id.btnDeleteAddress);
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_address_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int i) {
        Address a = list.get(i);

        h.rb.setText(a.fullAddress);

        h.rb.setOnClickListener(v ->
                ((AddressListActivity)context).setSelected(a));

        h.delete.setOnClickListener(v ->
                ((AddressListActivity)context).deleteAddress(a));

        h.edit.setOnClickListener(v ->
                ((AddressListActivity)context).openAddDialog(a));
    }

    @Override
    public int getItemCount() { return list.size(); }
}
