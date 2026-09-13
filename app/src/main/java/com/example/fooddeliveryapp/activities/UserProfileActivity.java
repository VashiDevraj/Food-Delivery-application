package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.fooddeliveryapp.databinding.ActivityUserProfileBinding;
import com.example.fooddeliveryapp.databinding.DialogAddressFormBinding;
import com.example.fooddeliveryapp.databinding.DialogEditProfileBinding;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityUserProfileBinding binding;
    private SessionManager             sessionManager;
    private FirebaseAuth               firebaseAuth;
    private DatabaseReference          dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        firebaseAuth   = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        populateCachedData();
        loadProfileFromFirebase();
        setupSwitchColors();
        setupClickListeners();

    }

    // ── Cache ──────────
    private void populateCachedData() {
        String name  = sessionManager.getName();
        String city  = sessionManager.getUserCity();
        String phone = sessionManager.getUserPhone();

        if (name != null && !name.isEmpty()) {
            binding.tvProfileName.setText(name);
            binding.tvProfileInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
        if (city  != null && !city.isEmpty())  binding.tvProfileCity.setText(city);
        if (phone != null && !phone.isEmpty()) binding.tvProfilePhone.setText(phone);
        else binding.tvProfilePhone.setText("Not set");

        if (firebaseAuth.getCurrentUser() != null) {
            String email = firebaseAuth.getCurrentUser().getEmail();
            binding.tvProfileEmail.setText(email != null ? email : "Not set");
        }

        binding.switchVegModeProfile.setChecked(sessionManager.isVegMode());
        binding.switchPersonalisedRatings.setChecked(
                getSharedPreferences("AppSettings", MODE_PRIVATE)
                        .getBoolean("personalised_ratings", false));
    }

    // ── Firebase ──────────────────────────────────────────────────────────────
    private void loadProfileFromFirebase() {
        String uid = sessionManager.getUid();
        if (uid == null || uid.isEmpty()) return;
        dbRef.child(Constants.NODE_USERS).child(uid).child(Constants.NODE_PROFILE)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        String name    = snapshot.child("name").getValue(String.class);
                        String phone   = snapshot.child("phone").getValue(String.class);
                        String city    = snapshot.child("city").getValue(String.class);
                        String address = snapshot.child("address").getValue(String.class);
                        String pincode = snapshot.child("pincode").getValue(String.class);

                        if (name != null && !name.isEmpty()) {
                            binding.tvProfileName.setText(name);
                            binding.tvProfileInitial.setText(
                                    String.valueOf(name.charAt(0)).toUpperCase());
                            sessionManager.saveUserName(name);
                        }
                        if (phone != null && !phone.isEmpty()) {
                            binding.tvProfilePhone.setText(phone);
                            sessionManager.saveUserPhone(phone);
                        } else {
                            binding.tvProfilePhone.setText("Not set");
                        }
                        if (city != null && !city.isEmpty()) {
                            binding.tvProfileCity.setText(city);
                            sessionManager.saveUserAddress(city,
                                    address != null ? address : city,
                                    pincode != null ? pincode : "");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }


    // ── Switch colors ─────────────────────────────────────────────────────────
    private void setupSwitchColors() {
        tintSwitch(binding.switchVegModeProfile,        binding.switchVegModeProfile.isChecked());
        tintSwitch(binding.switchPersonalisedRatings,   binding.switchPersonalisedRatings.isChecked());
    }

    private void tintSwitch(androidx.appcompat.widget.SwitchCompat sw, boolean on) {
        if (sw.getTrackDrawable() != null)
            sw.getTrackDrawable().setTint(on ? 0xFF4CAF50 : 0xFFAAAAAA);
    }

    // ── Click listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        binding.tvEditProfileLink.setOnClickListener(v -> showEditProfileDialog());

        binding.switchVegModeProfile.setOnCheckedChangeListener((btn, on) -> {
            sessionManager.setVegMode(on);
            tintSwitch(binding.switchVegModeProfile, on);
            savePreferenceToFirebase("vegMode", on);
            Toast.makeText(this, on ? "Veg mode ON 🥦" : "Veg mode OFF", Toast.LENGTH_SHORT).show();
        });

        binding.switchPersonalisedRatings.setOnCheckedChangeListener((btn, on) -> {
            getSharedPreferences("AppSettings", MODE_PRIVATE)
                    .edit().putBoolean("personalised_ratings", on).apply();
            tintSwitch(binding.switchPersonalisedRatings, on);
            savePreferenceToFirebase("personalisedRatings", on);
        });

        // ── Appearance: uses DarkModeManager ─────────────────────────────


        binding.llPaymentMethods.setOnClickListener(v -> showPaymentMethodsInfo());
        binding.llChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));
        binding.llSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        binding.llYourOrders.setOnClickListener(v ->
                startActivity(new Intent(this, OrderHistoryActivity.class)));
        binding.llAddressBook.setOnClickListener(v -> showAddressBookDialog());
        binding.llYourCollections.setOnClickListener(v ->
                startActivity(new Intent(this, FavouritesActivity.class)));
        binding.llOnlineHelp.setOnClickListener(v ->
                startActivity(new Intent(this, HelpSupportActivity.class)));
        binding.llAbout.setOnClickListener(v -> showAboutDialog());
        binding.llLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void savePreferenceToFirebase(String key, boolean value) {
        String uid = sessionManager.getUid();
        if (uid == null || uid.isEmpty()) return;
        dbRef.child(Constants.NODE_USERS).child(uid).child("preferences").child(key).setValue(value);
    }



    // ── Dialogs ───────────────────────────────────────────────────────────────



    private void showEditProfileDialog() {
        DialogEditProfileBinding form = DialogEditProfileBinding.inflate(LayoutInflater.from(this));
        String name  = binding.tvProfileName.getText().toString();
        String phone = binding.tvProfilePhone.getText().toString();
        if (!name.equals("Your Name"))   form.etEditName.setText(name);
        if (!phone.equals("Not set"))    form.etEditPhone.setText(phone);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("✏️  Edit Profile")
                .setView(form.getRoot())
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String n = form.etEditName.getText().toString().trim();
                    String p = form.etEditPhone.getText().toString().trim();
                    if (TextUtils.isEmpty(n)) { form.etEditName.setError("Name required"); return; }
                    if (!p.isEmpty() && p.length() < 10) { form.etEditPhone.setError("Enter 10-digit number"); return; }

                    binding.tvProfileName.setText(n);
                    binding.tvProfileInitial.setText(String.valueOf(n.charAt(0)).toUpperCase());
                    if (!p.isEmpty()) binding.tvProfilePhone.setText(p);
                    sessionManager.saveUserName(n);
                    if (!p.isEmpty()) sessionManager.saveUserPhone(p);

                    String uid = sessionManager.getUid();
                    if (uid != null && !uid.isEmpty()) {
                        Map<String, Object> upd = new HashMap<>();
                        upd.put("name", n);
                        if (!p.isEmpty()) upd.put("phone", p);
                        dbRef.child(Constants.NODE_USERS).child(uid).child(Constants.NODE_PROFILE)
                                .updateChildren(upd)
                                .addOnSuccessListener(u ->
                                        Toast.makeText(this, "Profile updated ✅", Toast.LENGTH_SHORT).show());
                    }
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showAddressBookDialog() {
        List<SessionManager.Address> addresses = sessionManager.getSavedAddresses();
        if (addresses.isEmpty()) { showSingleAddressFormDialog(-1, null); return; }

        String[] labels = new String[addresses.size() + 1];
        for (int i = 0; i < addresses.size(); i++) labels[i] = addresses.get(i).fullAddress;
        labels[addresses.size()] = "+ Add new address";
        int selected = sessionManager.getSelectedAddressIndex();

        new MaterialAlertDialogBuilder(this)
                .setTitle("📍 Your Addresses")
                .setSingleChoiceItems(labels, selected, null)
                .setPositiveButton("Select", (d, w) -> {
                    int idx = ((AlertDialog) d).getListView().getCheckedItemPosition();
                    if (idx == addresses.size()) {
                        showSingleAddressFormDialog(-1, null);
                    } else if (idx >= 0 && idx < addresses.size()) {
                        sessionManager.selectAddress(idx);
                        saveAddressToFirebase(addresses.get(idx));
                        binding.tvProfileCity.setText(addresses.get(idx).city);
                    }
                })
                .setNeutralButton("Add New", (d, w) -> showSingleAddressFormDialog(-1, null))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSingleAddressFormDialog(int editIndex, SessionManager.Address existing) {
        DialogAddressFormBinding form = DialogAddressFormBinding.inflate(LayoutInflater.from(this));
        boolean isEdit = editIndex >= 0 && existing != null;
        if (isEdit) {
            form.etHouseNo.setText(existing.houseNo);
            form.etStreet.setText(existing.street);
            form.etLandmark.setText(existing.landmark);
            form.etCity.setText(existing.city);
            form.etPincode.setText(existing.pincode);
        } else {
            String city = sessionManager.getUserCity();
            if (city != null) form.etCity.setText(city);
        }
        form.tvFormTitle.setText(isEdit ? "✏️  Edit Address" : "📍  Add New Address");
        form.btnSaveAddress.setText(isEdit ? "Update" : "Save Address");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(form.getRoot()).create();
        form.btnSaveAddress.setOnClickListener(v -> {
            String houseNo  = form.etHouseNo.getText().toString().trim();
            String street   = form.etStreet.getText().toString().trim();
            String landmark = form.etLandmark.getText().toString().trim();
            String city     = form.etCity.getText().toString().trim();
            String pincode  = form.etPincode.getText().toString().trim();
            if (TextUtils.isEmpty(houseNo))  { form.tilHouseNo.setError("Required"); return; }
            if (TextUtils.isEmpty(street))   { form.tilStreet.setError("Required"); return; }
            if (TextUtils.isEmpty(city))     { form.tilCity.setError("Required"); return; }
            if (pincode.length() != 6)       { form.tilPincode.setError("6-digit pincode required"); return; }
            String autoLabel = houseNo.length() > 12 ? houseNo.substring(0, 12) + "…" : houseNo;
            SessionManager.Address addr =
                    new SessionManager.Address(autoLabel, houseNo, street, landmark, city, pincode);
            if (isEdit) sessionManager.updateAddress(editIndex, addr);
            else        sessionManager.addAddress(addr);
            saveAddressToFirebase(addr);
            binding.tvProfileCity.setText(city);
            dialog.dismiss();
            Toast.makeText(this, "Address saved ✅", Toast.LENGTH_SHORT).show();
        });
        form.btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveAddressToFirebase(SessionManager.Address addr) {
        String uid = sessionManager.getUid();
        if (uid == null || uid.isEmpty()) return;
        Map<String, Object> upd = new HashMap<>();
        upd.put("address", addr.fullAddress);
        upd.put("city",    addr.city);
        upd.put("pincode", addr.pincode);
        dbRef.child(Constants.NODE_USERS).child(uid).child(Constants.NODE_PROFILE).updateChildren(upd);
    }

    private void showPaymentMethodsInfo() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("💳 Payment Methods")
                .setMessage("Supported payment methods:\n\n• Cash on Delivery (COD)\n• UPI — Google Pay, PhonePe, Paytm\n• Credit / Debit Card\n• Net Banking\n\nSelect your preferred method during checkout.")
                .setPositiveButton("OK", null).show();
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("ℹ️ About")
                .setMessage("Food Delivery App\nVersion 1.0.0\n\nA fast, reliable food delivery app.\n\nBuilt with ❤️ for food lovers.")
                .setPositiveButton("OK", null).show();
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out", (d, w) -> {
                    firebaseAuth.signOut();
                    sessionManager.logout();
                    Intent intent = new Intent(this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}