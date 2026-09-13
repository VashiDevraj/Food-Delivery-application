package com.example.fooddeliveryapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.activities.DeliveryBoyDashboardActivity;
import com.example.fooddeliveryapp.models.DeliveryBoy;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * DeliveryBoyAuthFragment — Login + Register for Delivery Partners.
 * Bicycle vehicle number field is hidden/optional (bicycles have no number plates).
 */
public class DeliveryBoyAuthFragment extends Fragment {

    private TabLayout         tabLoginRegister;
    private View              loginLayout, registerLayout;

    // Login
    private TextInputEditText etLoginEmail, etLoginPassword;
    private MaterialButton    btnLogin;

    // Register
    private TextInputEditText  etRegName, etRegEmail, etRegPhone, etRegPassword, etRegVehicleNumber;
    private TextInputLayout    tilVehicleNumber;   // wrapper layout for show/hide
    private Spinner            spinnerVehicleType;
    private MaterialButton     btnRegister;
    private TextView           tvVehicleNumberHint;

    private FirebaseAuth       auth;
    private DatabaseReference  dbRef;
    private SessionManager     sessionManager;

    private static final String BICYCLE = "Bicycle";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delivery_boy_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth           = FirebaseAuth.getInstance();
        dbRef          = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        sessionManager = new SessionManager(requireContext());

        tabLoginRegister = view.findViewById(R.id.tabDbLoginRegister);
        loginLayout      = view.findViewById(R.id.layoutDbLogin);
        registerLayout   = view.findViewById(R.id.layoutDbRegister);

        // Login
        etLoginEmail    = view.findViewById(R.id.etDbLoginEmail);
        etLoginPassword = view.findViewById(R.id.etDbLoginPassword);
        btnLogin        = view.findViewById(R.id.btnDbLogin);

        // Register
        etRegName           = view.findViewById(R.id.etDbRegName);
        etRegEmail          = view.findViewById(R.id.etDbRegEmail);
        etRegPhone          = view.findViewById(R.id.etDbRegPhone);
        etRegPassword       = view.findViewById(R.id.etDbRegPassword);
        etRegVehicleNumber  = view.findViewById(R.id.etDbRegVehicleNumber);
        tilVehicleNumber    = view.findViewById(R.id.tilVehicleNumber);
        spinnerVehicleType  = view.findViewById(R.id.spinnerVehicleType);
        btnRegister         = view.findViewById(R.id.btnDbRegister);
        tvVehicleNumberHint = view.findViewById(R.id.tvVehicleNumberHint);

        setupVehicleSpinner();

        tabLoginRegister.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loginLayout.setVisibility(View.VISIBLE);
                    registerLayout.setVisibility(View.GONE);
                } else {
                    loginLayout.setVisibility(View.GONE);
                    registerLayout.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnLogin.setOnClickListener(v -> loginDeliveryBoy());
        btnRegister.setOnClickListener(v -> registerDeliveryBoy());
    }

    private void setupVehicleSpinner() {
        String[] vehicleTypes = {"Scooter", "Bike", "Bicycle", "Car"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, vehicleTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(adapter);

        spinnerVehicleType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selected = vehicleTypes[position];
                if (BICYCLE.equals(selected)) {
                    // Bicycle — no number plate needed
                    tilVehicleNumber.setVisibility(View.GONE);
                    if (tvVehicleNumberHint != null) {
                        tvVehicleNumberHint.setVisibility(View.VISIBLE);
                        tvVehicleNumberHint.setText("🚲 Bicycles don't require a vehicle number");
                    }
                    // Clear any previous input
                    if (etRegVehicleNumber.getText() != null)
                        etRegVehicleNumber.getText().clear();
                } else {
                    tilVehicleNumber.setVisibility(View.VISIBLE);
                    if (tvVehicleNumberHint != null)
                        tvVehicleNumberHint.setVisibility(View.GONE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    private void loginDeliveryBoy() {
        String email    = getText(etLoginEmail);
        String password = getText(etLoginPassword);

        if (email.isEmpty())    { etLoginEmail.setError("Enter email"); return; }
        if (password.isEmpty()) { etLoginPassword.setError("Enter password"); return; }

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in…");

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) { resetLogin(); return; }

                    dbRef.child(Constants.NODE_DELIVERY_BOYS).child(user.getUid())
                            .child(Constants.NODE_PROFILE)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snap) {
                                    if (!snap.exists()) {
                                        Toast.makeText(getContext(),
                                                "No delivery partner account found. Please register.",
                                                Toast.LENGTH_LONG).show();
                                        auth.signOut();
                                        resetLogin();
                                        return;
                                    }
                                    String name          = snap.child("name").getValue(String.class);
                                    String phone         = snap.child("phone").getValue(String.class);
                                    String vehicleType   = snap.child("vehicleType").getValue(String.class);
                                    String vehicleNumber = snap.child("vehicleNumber").getValue(String.class);

                                    sessionManager.saveDeliveryBoySession(
                                            user.getUid(),
                                            name != null ? name : "",
                                            email,
                                            phone != null ? phone : "",
                                            vehicleType != null ? vehicleType : "",
                                            vehicleNumber != null ? vehicleNumber : "");

                                    // Refresh FCM token on every login
                                    FirebaseMessaging.getInstance().getToken()
                                            .addOnSuccessListener(token -> {
                                                if (token != null) {
                                                    dbRef.child(Constants.NODE_DELIVERY_BOYS)
                                                            .child(user.getUid())
                                                            .child(Constants.NODE_PROFILE)
                                                            .child("fcmToken").setValue(token);
                                                }
                                            });

                                    navigateToDashboard();
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError e) {
                                    Toast.makeText(getContext(),
                                            "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    resetLogin();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetLogin();
                });
    }

    // ── Register ──────────────────────────────────────────────────────────────
    private void registerDeliveryBoy() {
        String name          = getText(etRegName);
        String email         = getText(etRegEmail);
        String phone         = getText(etRegPhone);
        String password      = getText(etRegPassword);
        String vehicleType   = spinnerVehicleType.getSelectedItem().toString();
        boolean isBicycle    = BICYCLE.equals(vehicleType);
        // For bicycle, vehicle number is empty string (no plate needed)
        String vehicleNumber = isBicycle ? "" : getText(etRegVehicleNumber);

        if (name.isEmpty())        { etRegName.setError("Enter name"); return; }
        if (email.isEmpty())       { etRegEmail.setError("Enter email"); return; }
        if (phone.length() < 10)   { etRegPhone.setError("Enter valid 10-digit phone"); return; }
        if (password.length() < 6) { etRegPassword.setError("Password must be 6+ chars"); return; }
        if (!isBicycle && vehicleNumber.isEmpty()) {
            etRegVehicleNumber.setError("Enter vehicle number"); return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Registering…");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) { resetRegister(); return; }

                    Map<String, Object> profileData = new HashMap<>();
                    profileData.put("uid",             user.getUid());
                    profileData.put("name",            name);
                    profileData.put("email",           email);
                    profileData.put("phone",           phone);
                    profileData.put("vehicleType",     vehicleType);
                    profileData.put("vehicleNumber",   vehicleNumber);
                    profileData.put("isOnline",        false);
                    profileData.put("avgRating",       0.0);
                    profileData.put("totalRatings",    0);
                    profileData.put("totalDeliveries", 0);
                    profileData.put("totalEarnings",   0.0);
                    profileData.put("todayEarnings",   0.0);
                    profileData.put("monthEarnings",   0.0);
                    profileData.put("registeredAt",    System.currentTimeMillis());
                    profileData.put("role",            Constants.ROLE_DELIVERY_BOY);
                    profileData.put("fcmToken",        "");

                    dbRef.child(Constants.NODE_DELIVERY_BOYS).child(user.getUid())
                            .child(Constants.NODE_PROFILE)
                            .setValue(profileData)
                            .addOnSuccessListener(unused -> {
                                sessionManager.saveDeliveryBoySession(
                                        user.getUid(), name, email, phone, vehicleType, vehicleNumber);

                                // Save FCM token immediately after registration
                                FirebaseMessaging.getInstance().getToken()
                                        .addOnSuccessListener(token -> {
                                            if (token != null) {
                                                dbRef.child(Constants.NODE_DELIVERY_BOYS)
                                                        .child(user.getUid())
                                                        .child(Constants.NODE_PROFILE)
                                                        .child("fcmToken").setValue(token);
                                            }
                                        });

                                Toast.makeText(getContext(),
                                        "Welcome " + name + "! Account created ✅",
                                        Toast.LENGTH_SHORT).show();
                                navigateToDashboard();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(),
                                        "Profile save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                resetRegister();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetRegister();
                });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(requireContext(), DeliveryBoyDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void resetLogin() {
        if (btnLogin != null) { btnLogin.setEnabled(true); btnLogin.setText("Login"); }
    }

    private void resetRegister() {
        if (btnRegister != null) { btnRegister.setEnabled(true); btnRegister.setText("Create Account"); }
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}