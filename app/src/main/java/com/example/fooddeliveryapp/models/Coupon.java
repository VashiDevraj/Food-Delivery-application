package com.example.fooddeliveryapp.models;

/**
 * Coupon — represents a discount coupon with validation rules.
 *
 * Fields:
 *   code          — coupon code string (e.g. "WELCOME50")
 *   description   — human-readable description shown in UI
 *   discount      — flat ₹ amount deducted
 *   minOrderValue — minimum cart subtotal required to apply (0 = no minimum)
 *   isNewUserOnly — if true, only valid when user has 0 previous orders
 */
public class Coupon {

    private String  code;
    private String  description;
    private int     discount;
    private double  minOrderValue;   // ← NEW: min cart value to use this coupon
    private boolean isNewUserOnly;   // ← NEW: restrict to first-time orderers

    public Coupon() {}

    /** Full constructor */
    public Coupon(String code, String description, int discount,
                  double minOrderValue, boolean isNewUserOnly) {
        this.code          = code;
        this.description   = description;
        this.discount      = discount;
        this.minOrderValue = minOrderValue;
        this.isNewUserOnly = isNewUserOnly;
    }

    /** Legacy 3-arg constructor — kept so existing callers compile */
    public Coupon(String code, String description, int discount) {
        this(code, description, discount, 0, false);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getCode()          { return code; }
    public String  getDescription()   { return description; }
    public int     getDiscount()      { return discount; }
    public double  getMinOrderValue() { return minOrderValue; }
    public boolean isNewUserOnly()    { return isNewUserOnly; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setCode(String code)                   { this.code          = code; }
    public void setDescription(String description)     { this.description   = description; }
    public void setDiscount(int discount)              { this.discount      = discount; }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }
    public void setNewUserOnly(boolean newUserOnly)    { this.isNewUserOnly = newUserOnly; }
}