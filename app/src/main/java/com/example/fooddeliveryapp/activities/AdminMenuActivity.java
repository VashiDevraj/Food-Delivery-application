package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.databinding.ActivityAdminMenuBinding;
import com.example.fooddeliveryapp.models.FoodItem;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AdminMenuActivity extends AppCompatActivity {

    private ActivityAdminMenuBinding binding;
    private DatabaseReference         dbRef;
    private String                    adminId;
    private final List<FoodItem>      menuItems = new ArrayList<>();
    private MenuAdapter               adapter;

    // For edit dialog image pick
    private ActivityResultLauncher<Intent> editImageLauncher;
    private Bitmap   editSelectedBitmap;
    private ImageView editDialogIvPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adminId = new SessionManager(this).getUid();
        dbRef   = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Menu 🍽️");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        registerEditImageLauncher();
        setupRecyclerView();
        loadMenu();
    }

    private void registerEditImageLauncher() {
        editImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            InputStream s = getContentResolver()
                                    .openInputStream(result.getData().getData());
                            editSelectedBitmap = BitmapFactory.decodeStream(s);
                            if (editDialogIvPreview != null)
                                editDialogIvPreview.setImageBitmap(editSelectedBitmap);
                        } catch (Exception e) {
                            Toast.makeText(this, "Image load failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupRecyclerView() {
        adapter = new MenuAdapter(menuItems, this::showEditDialog, this::confirmDelete);
        binding.rvMenu.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMenu.setAdapter(adapter);
    }

    private void loadMenu() {
        binding.progressBar.setVisibility(View.VISIBLE);
        dbRef.child(Constants.NODE_ADMINS).child(adminId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_MENU)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        menuItems.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            FoodItem item = snap.getValue(FoodItem.class);
                            if (item != null) menuItems.add(item);
                        }
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(menuItems.isEmpty() ? View.VISIBLE : View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    // ── Edit Dialog ───────────────────────────────────────────────────────────
    private void showEditDialog(FoodItem item) {
        editSelectedBitmap = null;
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_food, null);

        TextInputEditText etName  = v.findViewById(R.id.etEditFoodName);
        TextInputEditText etDesc  = v.findViewById(R.id.etEditFoodDesc);
        TextInputEditText etPrice = v.findViewById(R.id.etEditFoodPrice);
        androidx.appcompat.widget.SwitchCompat switchVeg = v.findViewById(R.id.switchEditIsVeg);
        androidx.appcompat.widget.SwitchCompat switchAvail = v.findViewById(R.id.switchEditAvailable);
        editDialogIvPreview = v.findViewById(R.id.ivEditFoodPreview);
        MaterialButton btnChangeImage = v.findViewById(R.id.btnEditChangeImage);

        etName.setText(item.getName());
        etDesc.setText(item.getDescription());
        etPrice.setText(String.valueOf(item.getPrice()));
        switchVeg.setChecked(item.isVeg());
        switchAvail.setChecked(item.isAvailable());

        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            try {
                byte[] bytes = Base64.decode(item.getImageBase64(), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) editDialogIvPreview.setImageBitmap(bmp);
            } catch (Exception ignored) {}
        }

        btnChangeImage.setOnClickListener(b -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            editImageLauncher.launch(intent);
        });

        new AlertDialog.Builder(this)
                .setTitle("Edit Item")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    String name     = etName.getText()  != null ? etName.getText().toString().trim()  : "";
                    String desc     = etDesc.getText()  != null ? etDesc.getText().toString().trim()  : "";
                    String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
                    boolean isVeg   = switchVeg.isChecked();
                    boolean avail   = switchAvail.isChecked();

                    if (name.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(this, "Name and price required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double price;
                    try { price = Double.parseDouble(priceStr); }
                    catch (NumberFormatException ex) {
                        Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show(); return;
                    }

                    String imageBase64 = item.getImageBase64() != null ? item.getImageBase64() : "";
                    if (editSelectedBitmap != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        editSelectedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                    }

                    item.setName(name);
                    item.setDescription(desc);
                    item.setPrice(price);
                    item.setVeg(isVeg);
                    item.setAvailable(avail);
                    item.setImageBase64(imageBase64);
                    updateFoodItem(item);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateFoodItem(FoodItem item) {
        DatabaseReference menuRef = dbRef.child(Constants.NODE_ADMINS).child(adminId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_MENU)
                .child(item.getFoodId());
        menuRef.setValue(item)
                .addOnSuccessListener(u -> {
                    dbRef.child(Constants.NODE_FOOD_ITEMS).child(item.getFoodId()).setValue(item);
                    Toast.makeText(this, item.getName() + " updated ✅", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDelete(FoodItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Delete \"" + item.getName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteFoodItem(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFoodItem(FoodItem item) {
        dbRef.child(Constants.NODE_ADMINS).child(adminId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_MENU)
                .child(item.getFoodId()).removeValue()
                .addOnSuccessListener(u -> {
                    dbRef.child(Constants.NODE_FOOD_ITEMS).child(item.getFoodId()).removeValue();
                    Toast.makeText(this, item.getName() + " deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    static class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.VH> {
        interface OnEdit   { void onEdit(FoodItem item); }
        interface OnDelete { void onDelete(FoodItem item); }

        private final List<FoodItem> items;
        private final OnEdit   onEdit;
        private final OnDelete onDelete;

        MenuAdapter(List<FoodItem> items, OnEdit onEdit, OnDelete onDelete) {
            this.items    = items;
            this.onEdit   = onEdit;
            this.onDelete = onDelete;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_menu, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            FoodItem item = items.get(pos);
            h.tvName.setText(item.getName());
            h.tvPrice.setText("₹" + String.format("%.0f", item.getPrice()));
            h.tvBadge.setText(item.isVeg() ? "🌿 Veg" : "🍖 Non-Veg");
            h.tvAvail.setText(item.isAvailable() ? "✅ Available" : "❌ Hidden");
            h.tvAvail.setTextColor(item.isAvailable() ? 0xFF4CAF50 : 0xFFE23744);

            if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(item.getImageBase64(), Base64.DEFAULT);
                    Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp != null) { h.ivFood.setImageBitmap(bmp); }
                    else h.ivFood.setImageResource(R.drawable.ic_food_placeholder);
                } catch (Exception e) {
                    h.ivFood.setImageResource(R.drawable.ic_food_placeholder);
                }
            } else {
                h.ivFood.setImageResource(R.drawable.ic_food_placeholder);
            }

            h.btnEdit.setOnClickListener(v -> onEdit.onEdit(item));
            h.btnDelete.setOnClickListener(v -> onDelete.onDelete(item));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivFood;
            TextView  tvName, tvPrice, tvBadge, tvAvail;
            MaterialButton btnEdit, btnDelete;
            VH(@NonNull View v) {
                super(v);
                ivFood    = v.findViewById(R.id.ivMenuFoodImage);
                tvName    = v.findViewById(R.id.tvMenuFoodName);
                tvPrice   = v.findViewById(R.id.tvMenuFoodPrice);
                tvBadge   = v.findViewById(R.id.tvMenuVegBadge);
                tvAvail   = v.findViewById(R.id.tvMenuAvailable);
                btnEdit   = v.findViewById(R.id.btnMenuEdit);
                btnDelete = v.findViewById(R.id.btnMenuDelete);
            }
        }
    }
}