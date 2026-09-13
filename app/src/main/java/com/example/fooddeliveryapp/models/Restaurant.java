package com.example.fooddeliveryapp.models;

import com.example.fooddeliveryapp.utils.RestaurantStatusHelper;

public class Restaurant {

    private String restaurantId;
    private String name;
    private String city;
    private String address;
    private String pincode;
    private String phone;
    private String imageBase64;
    private double rating;
    private String deliveryTime;
    private boolean vegOnly;
    private String offer;

    // 🔥 ADD THESE FIELDS (MISSING)
    private boolean isOpen = true;
    private boolean manualOverride = false;
    private String openTime = "09:00";
    private String closeTime = "22:00";

    public Restaurant() {}

    // ── Getters ─────────────────────────

    public String getRestaurantId() { return restaurantId; }
    public String getName() { return name != null ? name : ""; }
    public String getCity() { return city != null ? city : ""; }
    public String getAddress() { return address != null ? address : ""; }
    public String getPincode() { return pincode != null ? pincode : ""; }
    public String getPhone() { return phone != null ? phone : ""; }
    public String getImageBase64() { return imageBase64 != null ? imageBase64 : ""; }
    public double getRating() { return rating; }
    public String getDeliveryTime() { return deliveryTime != null ? deliveryTime : "30-45 min"; }
    public boolean isVegOnly() { return vegOnly; }
    public String getOffer() { return offer != null ? offer : ""; }

    // 🔥 REQUIRED GETTERS
    public boolean isOpen() { return isOpen; }
    public boolean isManualOverride() { return manualOverride; }
    public String getOpenTime() { return openTime != null ? openTime : "09:00"; }
    public String getCloseTime() { return closeTime != null ? closeTime : "22:00"; }

    // 🔥 IMPORTANT (USED IN UI)
    public String getOpensAtText() {
        return "Opens at " + RestaurantStatusHelper.formatTime(getOpenTime());
    }

    // ── Setters ─────────────────────────

    public void setRestaurantId(String id) { this.restaurantId = id; }
    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setAddress(String address) { this.address = address; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setImageBase64(String img) { this.imageBase64 = img; }
    public void setRating(double rating) { this.rating = rating; }
    public void setDeliveryTime(String t) { this.deliveryTime = t; }
    public void setVegOnly(boolean vegOnly) { this.vegOnly = vegOnly; }
    public void setOffer(String offer) { this.offer = offer; }

    // 🔥 REQUIRED SETTERS
    public void setOpen(boolean open) { this.isOpen = open; }
    public void setManualOverride(boolean manualOverride) { this.manualOverride = manualOverride; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
}