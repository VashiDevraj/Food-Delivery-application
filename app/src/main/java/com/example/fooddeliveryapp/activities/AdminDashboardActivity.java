package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.databinding.ActivityAdminDashboardBinding;
import com.example.fooddeliveryapp.models.FoodItem;
import com.example.fooddeliveryapp.models.Restaurant;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.RestaurantStatusHelper;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private DatabaseReference dbRef;
    private SessionManager    sessionManager;
    private String            adminId;

    // ── Restaurant state ──────────────────────────────────────────────────────
    private String  restaurantId    = "";
    private boolean isOpen          = true;
    private boolean manualOverride  = false;
    private String  openTime        = "09:00";
    private String  closeTime       = "22:00";

    // ── Image pickers ─────────────────────────────────────────────────────────
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Bitmap                         selectedFoodBitmap;
    private android.widget.ImageView       dialogIvPreview;
    private View                           dialogLayoutPlaceholder;

    private ActivityResultLauncher<Intent> categoryImagePickerLauncher;
    private Bitmap                         selectedCategoryBitmap;
    private android.widget.ImageView       dialogCatIvPreview;
    private View                           dialogCatLayoutPlaceholder;

    // ── Category helpers ──────────────────────────────────────────────────────
    private final List<String> categoryNames = new ArrayList<>();
    private final List<String> categoryIds   = new ArrayList<>();

    private int lastOrderCount = -1;

    // ── Status listener (live) ────────────────────────────────────────────────
    private ValueEventListener statusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        adminId        = sessionManager.getUid();

        if (adminId == null || adminId.isEmpty()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        dbRef = FirebaseDatabase
                .getInstance(Constants.FIREBASE_URL)
                .getReference();

        registerImagePicker();
        registerCategoryImagePicker();
        setupWelcome();
        setupClickListeners();

        loadRestaurantInfo();
        loadOrderStats();
        loadMenuItemCount();
        listenForNewOrders();
        loadCategoryCount();
        loadCategoriesForDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null && !restaurantId.isEmpty()) {
            dbRef.child(Constants.NODE_RESTAURANTS).child(restaurantId)
                    .removeEventListener(statusListener);
        }
    }

    // ── Image Pickers ─────────────────────────────────────────────────────────

    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            InputStream stream = getContentResolver()
                                    .openInputStream(result.getData().getData());
                            selectedFoodBitmap = BitmapFactory.decodeStream(stream);
                            if (dialogIvPreview != null)
                                dialogIvPreview.setImageBitmap(selectedFoodBitmap);
                            if (dialogLayoutPlaceholder != null)
                                dialogLayoutPlaceholder.setVisibility(View.GONE);
                        } catch (Exception e) {
                            Toast.makeText(this, "Image load failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void registerCategoryImagePicker() {
        categoryImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        try {
                            InputStream stream = getContentResolver()
                                    .openInputStream(result.getData().getData());
                            selectedCategoryBitmap = BitmapFactory.decodeStream(stream);
                            if (dialogCatIvPreview != null)
                                dialogCatIvPreview.setImageBitmap(selectedCategoryBitmap);
                            if (dialogCatLayoutPlaceholder != null)
                                dialogCatLayoutPlaceholder.setVisibility(View.GONE);
                        } catch (Exception e) {
                            Toast.makeText(this, "Image load failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadCategoriesForDialog() {
        dbRef.child(Constants.NODE_CATEGORIES)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        categoryNames.clear(); categoryIds.clear();
                        categoryNames.add("None"); categoryIds.add("");
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String name = snap.child("categoryName").getValue(String.class);
                            String id   = snap.child("categoryId").getValue(String.class);
                            if (name != null) {
                                categoryNames.add(name);
                                categoryIds.add(id != null ? id : "");
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void setupWelcome() {
        String name = sessionManager.getName();
        if (name != null && !name.isEmpty()) {
            binding.tvAdminName.setText("Welcome, " + name);
            binding.tvInitialLetter.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
    }

    private void setupClickListeners() {
        binding.tvAdminInitial.setOnClickListener(v ->
                startActivity(new Intent(this, AdminProfileActivity.class)));

        binding.btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminOrdersActivity.class));
            binding.tvNewOrderBadge.setVisibility(View.GONE);
        });
        binding.btnAddFood.setOnClickListener(v -> showAddFoodDialog());
        binding.btnManageOrders.setOnClickListener(v ->
                startActivity(new Intent(this, AdminOrdersActivity.class)));
        binding.btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        binding.btnEditRestaurant.setOnClickListener(v ->
                startActivity(new Intent(this, RestaurantEditActivity.class)));
        binding.btnManageMenu.setOnClickListener(v ->
                startActivity(new Intent(this, AdminMenuActivity.class)));
        binding.btnBestSellers.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBestSellersActivity.class)));
        binding.btnReviews.setOnClickListener(v ->
                startActivity(new Intent(this, AdminReviewsActivity.class)));

        // ── Open/Close manual toggle ──────────────────────────────────────────
        binding.switchRestaurantOpen.setOnCheckedChangeListener((btn, checked) -> {
            if (!restaurantId.isEmpty()) {
                isOpen         = checked;
                manualOverride = true;
                persistStatusToFirebase();
                updateStatusUI();
                Toast.makeText(this,
                        checked ? "✅ Restaurant set to OPEN" : "🔴 Restaurant set to CLOSED",
                        Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSetSchedule.setOnClickListener(v -> showScheduleDialog());

        binding.btnAutoSchedule.setOnClickListener(v -> {
            if (!restaurantId.isEmpty()) {
                manualOverride = false;
                isOpen = RestaurantStatusHelper.isInSchedule(openTime, closeTime);
                persistStatusToFirebase();
                updateStatusUI();
                Toast.makeText(this, "⏰ Auto-schedule enabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attachStatusListener() {
        if (restaurantId.isEmpty()) return;
        if (statusListener != null) {
            dbRef.child(Constants.NODE_RESTAURANTS).child(restaurantId)
                    .removeEventListener(statusListener);
        }
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Boolean openVal    = snapshot.child("isOpen").getValue(Boolean.class);
                Boolean manualVal  = snapshot.child("manualOverride").getValue(Boolean.class);
                String  openT      = snapshot.child("openTime").getValue(String.class);
                String  closeT     = snapshot.child("closeTime").getValue(String.class);

                if (openVal   != null) isOpen         = openVal;
                if (manualVal != null) manualOverride  = manualVal;
                if (openT     != null && !openT.isEmpty())  openTime  = openT;
                if (closeT    != null && !closeT.isEmpty()) closeTime = closeT;

                updateStatusUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        dbRef.child(Constants.NODE_RESTAURANTS).child(restaurantId)
                .addValueEventListener(statusListener);
    }

    private void persistStatusToFirebase() {
        if (restaurantId.isEmpty()) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("isOpen",         isOpen);
        updates.put("manualOverride", manualOverride);
        updates.put("openTime",       openTime);
        updates.put("closeTime",      closeTime);
        dbRef.child(Constants.NODE_RESTAURANTS).child(restaurantId).updateChildren(updates);
        dbRef.child(Constants.NODE_ADMINS).child(adminId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_RESTAURANT_INFO)
                .updateChildren(updates);
    }

    private void updateStatusUI() {
        boolean effectivelyOpen = RestaurantStatusHelper.isOpen(isOpen, manualOverride, openTime, closeTime);

        binding.switchRestaurantOpen.setOnCheckedChangeListener(null);
        binding.switchRestaurantOpen.setChecked(effectivelyOpen);
        binding.switchRestaurantOpen.setOnCheckedChangeListener((btn, checked) -> {
            if (!restaurantId.isEmpty()) {
                isOpen = checked; manualOverride = true;
                persistStatusToFirebase();
                updateStatusUI();
                Toast.makeText(this,
                        checked ? "✅ Restaurant set to OPEN" : "🔴 Restaurant set to CLOSED",
                        Toast.LENGTH_SHORT).show();
            }
        });

        if (effectivelyOpen) {
            binding.tvRestaurantStatus.setText("🟢 OPEN");
            binding.tvRestaurantStatus.setTextColor(0xFF2E7D32);
            binding.cardRestaurantStatus.setCardBackgroundColor(0xFFE8F5E9);
            binding.tvStatusSubtitle.setText("Closes at " + RestaurantStatusHelper.formatTime(closeTime));
        } else {
            binding.tvRestaurantStatus.setText("🔴 CLOSED");
            binding.tvRestaurantStatus.setTextColor(0xFFB71C1C);
            binding.cardRestaurantStatus.setCardBackgroundColor(0xFFFFEBEE);
            binding.tvStatusSubtitle.setText("Opens at " + RestaurantStatusHelper.formatTime(openTime));
        }

        binding.tvScheduleInfo.setText("📅  "
                + RestaurantStatusHelper.formatTime(openTime)
                + "  –  "
                + RestaurantStatusHelper.formatTime(closeTime));

        binding.tvOverrideLabel.setText(manualOverride ? "⚙ Manual override ON" : "⏰ Auto-schedule");
        binding.tvOverrideLabel.setTextColor(manualOverride ? 0xFFE65100 : 0xFF1565C0);
    }

    private void showScheduleDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_set_schedule, null);

        TimePicker tpOpen  = dialogView.findViewById(R.id.timePickerOpen);
        TimePicker tpClose = dialogView.findViewById(R.id.timePickerClose);
        tpOpen.setIs24HourView(false);
        tpClose.setIs24HourView(false);

        int[] openParts  = parseTimeParts(openTime);
        int[] closeParts = parseTimeParts(closeTime);
        tpOpen.setHour(openParts[0]);   tpOpen.setMinute(openParts[1]);
        tpClose.setHour(closeParts[0]); tpClose.setMinute(closeParts[1]);

        new AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                .setTitle("⏰  Set Opening Hours")
                .setView(dialogView)
                .setPositiveButton("Save & Auto Mode", (d, w) -> {
                    openTime  = String.format("%02d:%02d", tpOpen.getHour(),  tpOpen.getMinute());
                    closeTime = String.format("%02d:%02d", tpClose.getHour(), tpClose.getMinute());
                    manualOverride = false;
                    isOpen = RestaurantStatusHelper.isInSchedule(openTime, closeTime);
                    persistStatusToFirebase();
                    updateStatusUI();
                    Toast.makeText(this, "✅ Schedule saved · Auto mode ON", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Save (Keep Override)", (d, w) -> {
                    openTime  = String.format("%02d:%02d", tpOpen.getHour(),  tpOpen.getMinute());
                    closeTime = String.format("%02d:%02d", tpClose.getHour(), tpClose.getMinute());
                    persistStatusToFirebase();
                    updateStatusUI();
                    Toast.makeText(this, "✅ Schedule saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int[] parseTimeParts(String hhmm) {
        try {
            String[] p = hhmm.split(":");
            return new int[]{Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())};
        } catch (Exception e) {
            return new int[]{9, 0};
        }
    }

    private void loadRestaurantInfo() {
        dbRef.child(Constants.NODE_ADMINS).child(adminId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_RESTAURANT_INFO)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        Restaurant r = snapshot.getValue(Restaurant.class);
                        if (r != null) {
                            binding.tvRestaurantName.setText(r.getName());
                            binding.tvRestaurantCity.setText(r.getCity() + " · " + r.getDeliveryTime());
                            binding.tvVegBadge.setText(r.isVegOnly() ? "🌿 Pure Veg" : "🍖 Veg & Non-Veg");
                            isOpen        = r.isOpen();
                            manualOverride = r.isManualOverride();
                            openTime      = r.getOpenTime();
                            closeTime     = r.getCloseTime();
                        }
                        loadRestaurantId();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadRestaurantId() {
        dbRef.child(Constants.NODE_RESTAURANTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String rid = snap.child("restaurantId").getValue(String.class);
                            if (adminId.equals(rid) || adminId.equals(snap.getKey())) {
                                restaurantId = snap.getKey();
                                break;
                            }
                        }
                        if (restaurantId.isEmpty()) restaurantId = adminId;
                        attachStatusListener();
                        updateStatusUI();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        restaurantId = adminId;
                        attachStatusListener();
                        updateStatusUI();
                    }
                });
    }

    // ── Stats — NET earnings after delivery costs ─────────────────────────────

    private void loadOrderStats() {
        dbRef.child(Constants.NODE_ORDERS)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long   total          = 0;
                        long   pending        = 0;
                        double grossRevenue   = 0;       // total order amounts
                        double grossToday     = 0;
                        double totalDeliveryCost = 0;    // delivery boy commissions + tips
                        double todayDeliveryCost = 0;
                        long   startOfToday   = getTodayStartMillis();

                        for (DataSnapshot order : snapshot.getChildren()) {
                            String rid = order.child("restaurantId").getValue(String.class);
                            if (!adminId.equals(rid)) continue;

                            // Only count delivered orders in revenue
                            String status = order.child("status").getValue(String.class);
                            if (status != null && (status.equalsIgnoreCase("pending")
                                    || status.equalsIgnoreCase("placed"))) {
                                pending++;
                            }
                            total++;

                            Double amt = order.child("totalAmount").getValue(Double.class);
                            if (amt == null) amt = order.child("totalPrice").getValue(Double.class);
                            if (amt == null) {
                                Long l = order.child("totalAmount").getValue(Long.class);
                                if (l == null) l = order.child("totalPrice").getValue(Long.class);
                                if (l != null) amt = l.doubleValue();
                            }
                            if (amt == null) amt = 0.0;

                            Double tip = order.child("tipAmount").getValue(Double.class);
                            if (tip == null) tip = 0.0;

                            Long ts = order.child("timestamp").getValue(Long.class);

                            // Delivery cost = BASE_FEE + (orderAmount * commission%) + tip
                            // Only if a delivery boy was assigned
                            String dbId = order.child("deliveryBoyId").getValue(String.class);
                            double deliveryCost = 0;
                            if (dbId != null && !dbId.isEmpty()) {
                                deliveryCost = Constants.BASE_DELIVERY_FEE
                                        + (amt * Constants.DELIVERY_COMMISSION_PERCENT / 100.0)
                                        + tip;
                            }

                            grossRevenue     += amt;
                            totalDeliveryCost+= deliveryCost;

                            if (ts != null && ts >= startOfToday) {
                                grossToday       += amt;
                                todayDeliveryCost+= deliveryCost;
                            }
                        }

                        // NET = Gross revenue − delivery boy costs
                        double netRevenue = grossRevenue - totalDeliveryCost;
                        double netToday   = grossToday   - todayDeliveryCost;

                        binding.tvTotalOrders.setText(String.valueOf(total));
                        binding.tvPendingOrders.setText(String.valueOf(pending));
                        // Show net earnings (after delivery costs)
                        binding.tvTodayRevenue.setText("₹" + String.format("%.0f", Math.max(0, netToday)));
                        binding.tvTotalRevenue.setText("₹" + String.format("%.0f", Math.max(0, netRevenue)));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private long getTodayStartMillis() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void loadMenuItemCount() {
        dbRef.child(Constants.NODE_ADMINS).child(adminId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_MENU)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        binding.tvTotalFoodItems.setText(String.valueOf(s.getChildrenCount()));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadCategoryCount() {
        dbRef.child(Constants.NODE_CATEGORIES)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        binding.tvTotalCategories.setText(String.valueOf(s.getChildrenCount()));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void listenForNewOrders() {
        dbRef.child(Constants.NODE_ORDERS)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 0;
                        for (DataSnapshot o : snapshot.getChildren()) {
                            String rid = o.child("restaurantId").getValue(String.class);
                            if (adminId.equals(rid)) count++;
                        }
                        if (lastOrderCount >= 0 && count > lastOrderCount) {
                            int diff = count - lastOrderCount;
                            binding.tvNewOrderBadge.setVisibility(View.VISIBLE);
                            binding.tvBadgeCount.setText(diff > 9 ? "9+" : String.valueOf(diff));
                        }
                        lastOrderCount = count;
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Add Food Dialog ───────────────────────────────────────────────────────

    private void showAddFoodDialog() {
        selectedFoodBitmap = null;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_food, null);

        com.google.android.material.textfield.TextInputEditText etName =
                dialogView.findViewById(R.id.etFoodName);
        com.google.android.material.textfield.TextInputEditText etDesc =
                dialogView.findViewById(R.id.etFoodDescription);
        com.google.android.material.textfield.TextInputEditText etPrice =
                dialogView.findViewById(R.id.etFoodPrice);
        androidx.appcompat.widget.SwitchCompat switchVeg =
                dialogView.findViewById(R.id.switchIsVeg);
        dialogIvPreview         = dialogView.findViewById(R.id.ivFoodImagePreview);
        dialogLayoutPlaceholder = dialogView.findViewById(R.id.layoutImagePlaceholder);
        com.google.android.material.card.MaterialCardView cardImage =
                dialogView.findViewById(R.id.cardFoodImage);

        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoryNames);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        cardImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        new AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                .setTitle("🍽  Add Menu Item")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, w) -> {
                    String name     = etName.getText()  != null ? etName.getText().toString().trim()  : "";
                    String desc     = etDesc.getText()  != null ? etDesc.getText().toString().trim()  : "";
                    String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
                    boolean isVeg   = switchVeg.isChecked();
                    int selectedPos  = spinnerCategory.getSelectedItemPosition();
                    String catId     = categoryIds.get(selectedPos);

                    if (name.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(this, "Name and price are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double price;
                    try { price = Double.parseDouble(priceStr); }
                    catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show(); return;
                    }
                    String imageBase64 = "";
                    if (selectedFoodBitmap != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        selectedFoodBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                    }
                    saveFoodItem(name, desc, price, isVeg, imageBase64, catId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveFoodItem(String name, String desc, double price,
                              boolean isVeg, String imageBase64, String categoryId) {
        DatabaseReference menuRef = dbRef.child(Constants.NODE_ADMINS).child(adminId)
                .child(Constants.NODE_RESTAURANT).child(Constants.NODE_MENU);
        String foodId = menuRef.push().getKey();
        if (foodId == null) return;
        FoodItem item = new FoodItem(foodId, adminId, categoryId, name, desc, price, imageBase64, isVeg);
        menuRef.child(foodId).setValue(item)
                .addOnSuccessListener(u -> {
                    dbRef.child(Constants.NODE_FOOD_ITEMS).child(foodId).setValue(item);
                    Toast.makeText(this, name + " added ✅", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Add Category Dialog ───────────────────────────────────────────────────

    private void showAddCategoryDialog() {
        selectedCategoryBitmap = null;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);

        com.google.android.material.textfield.TextInputEditText etName =
                dialogView.findViewById(R.id.etCategoryName);
        dialogCatIvPreview         = dialogView.findViewById(R.id.ivCategoryImagePreview);
        dialogCatLayoutPlaceholder = dialogView.findViewById(R.id.layoutImagePlaceholder);

        com.google.android.material.card.MaterialCardView cardImage =
                dialogView.findViewById(R.id.cardCategoryImage);
        com.google.android.material.button.MaterialButton btnSelect =
                dialogView.findViewById(R.id.btnSelectCategoryImage);

        View.OnClickListener pickImage = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            categoryImagePickerLauncher.launch(intent);
        };
        cardImage.setOnClickListener(pickImage);
        btnSelect.setOnClickListener(pickImage);

        new AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                .setTitle("📁  Add Category")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, w) -> {
                    String name = etName.getText() != null
                            ? etName.getText().toString().trim() : "";
                    if (!name.isEmpty()) {
                        saveCategory(name, selectedCategoryBitmap);
                    } else {
                        Toast.makeText(this, "Enter a category name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveCategory(String name, Bitmap image) {
        String id = dbRef.child(Constants.NODE_CATEGORIES).push().getKey();
        if (id == null) return;
        String imageBase64 = "";
        if (image != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("categoryName",  name);
        map.put("categoryId",    id);
        map.put("categoryImage", imageBase64);
        dbRef.child(Constants.NODE_CATEGORIES).child(id).setValue(map)
                .addOnSuccessListener(u ->
                        Toast.makeText(this, "\"" + name + "\" added ✅", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}