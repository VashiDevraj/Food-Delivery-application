package com.example.fooddeliveryapp.models;

public class Address {

    public String addressId;
    public String label;
    public String fullAddress;
    public String city;
    public String pincode;

    // Required empty constructor for Firebase
    public Address() {}

    public Address(String addressId, String label, String fullAddress,
                   String city, String pincode) {
        this.addressId = addressId;
        this.label = label;
        this.fullAddress = fullAddress;
        this.city = city;
        this.pincode = pincode;
    }

    // Getters
    public String getAddressId() { return addressId; }
    public String getLabel() { return label; }
    public String getFullAddress() { return fullAddress; }
    public String getCity() { return city; }
    public String getPincode() { return pincode; }

    // Setters
    public void setAddressId(String addressId) { this.addressId = addressId; }
    public void setLabel(String label) { this.label = label; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
    public void setCity(String city) { this.city = city; }
    public void setPincode(String pincode) { this.pincode = pincode; }
}