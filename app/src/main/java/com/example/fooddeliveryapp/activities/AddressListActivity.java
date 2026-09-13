package com.example.fooddeliveryapp.activities;
import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.adapters.AddressAdapter;
import com.example.fooddeliveryapp.models.Address;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddressListActivity extends AppCompatActivity {

    RecyclerView rvAddresses;
    MaterialButton btnAddAddress;

    DatabaseReference dbRef;
    SessionManager sessionManager;

    List<Address> list = new ArrayList<>();
    AddressAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_list);

        rvAddresses = findViewById(R.id.rvAddresses);
        btnAddAddress = findViewById(R.id.btnAddAddress);

        sessionManager = new SessionManager(this);
        dbRef = FirebaseDatabase.getInstance().getReference();

        adapter = new AddressAdapter(this, list);
        rvAddresses.setAdapter(adapter);
        rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        loadAddresses();

        btnAddAddress.setOnClickListener(v -> openAddDialog(null));
    }

    private void loadAddresses() {
        String uid = sessionManager.getUid();

        dbRef.child("Users").child(uid).child("Addresses")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        list.clear();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            Address a = s.getValue(Address.class);
                            if (a != null) list.add(a);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    public void setSelected(Address address) {
        String uid = sessionManager.getUid();

        // Save in Firebase
        dbRef.child("Users").child(uid)
                .child("selectedAddressId")
                .setValue(address.addressId);

        // 🔥 ALSO SAVE IN SESSION (IMPORTANT)
        sessionManager.saveUserAddress(
                address.city,
                address.fullAddress,
                address.pincode
        );

        finish();
    }

    public void deleteAddress(Address address) {
        String uid = sessionManager.getUid();

        dbRef.child("Users").child(uid)
                .child("Addresses")
                .child(address.addressId)
                .removeValue();
    }

    public void openAddDialog(Address editAddress) {
        // reuse your dialog_address_form.xml :contentReference[oaicite:1]{index=1}
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_address_form);

        TextInputEditText etHouse = dialog.findViewById(R.id.etHouseNo);
        TextInputEditText etStreet = dialog.findViewById(R.id.etStreet);
        TextInputEditText etCity = dialog.findViewById(R.id.etCity);
        TextInputEditText etPin = dialog.findViewById(R.id.etPincode);

        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveAddress);

        btnSave.setOnClickListener(v -> {
            String full = etHouse.getText().toString() + ", " +
                    etStreet.getText().toString() + ", " +
                    etCity.getText().toString() + " - " +
                    etPin.getText().toString();

            String id = dbRef.child("Users")
                    .child(sessionManager.getUid())
                    .child("Addresses")
                    .push()
                    .getKey();

            Address a = new Address(id, "Home", full,
                    etCity.getText().toString(),
                    etPin.getText().toString());

            dbRef.child("Users")
                    .child(sessionManager.getUid())
                    .child("Addresses")
                    .child(id)
                    .setValue(a);
            // Save to session also
            sessionManager.saveUserAddress(
                    a.city,
                    a.fullAddress,
                    a.pincode
            );

            dialog.dismiss();
        });

        dialog.show();

// 🔥 MAKE FULL WIDTH
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
