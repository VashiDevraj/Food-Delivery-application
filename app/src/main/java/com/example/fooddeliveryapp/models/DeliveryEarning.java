package com.example.fooddeliveryapp.models;

/**
 * DeliveryEarning — stored at DeliveryBoys/{uid}/EarningHistory/{earningId}
 */
public class DeliveryEarning {

    private String earningId;
    private String orderId;
    private String restaurantName;
    private String customerName;
    private double orderAmount;
    private double tipAmount;
    private double commission;      // BASE_FEE + (orderAmount * commission%)
    private double totalEarning;    // commission + tip
    private long   timestamp;
    private String date;            // formatted date string for grouping

    public DeliveryEarning() {}

    public DeliveryEarning(String earningId, String orderId, String restaurantName,
                           String customerName, double orderAmount,
                           double tipAmount, double commission) {
        this.earningId      = earningId;
        this.orderId        = orderId;
        this.restaurantName = restaurantName;
        this.customerName   = customerName;
        this.orderAmount    = orderAmount;
        this.tipAmount      = tipAmount;
        this.commission     = commission;
        this.totalEarning   = commission + tipAmount;
        this.timestamp      = System.currentTimeMillis();
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getEarningId()      { return earningId      != null ? earningId      : ""; }
    public String getOrderId()        { return orderId        != null ? orderId        : ""; }
    public String getRestaurantName() { return restaurantName != null ? restaurantName : ""; }
    public String getCustomerName()   { return customerName   != null ? customerName   : ""; }
    public double getOrderAmount()    { return orderAmount; }
    public double getTipAmount()      { return tipAmount; }
    public double getCommission()     { return commission; }
    public double getTotalEarning()   { return totalEarning; }
    public long   getTimestamp()      { return timestamp; }
    public String getDate()           { return date           != null ? date           : ""; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setEarningId(String earningId)           { this.earningId      = earningId; }
    public void setOrderId(String orderId)               { this.orderId        = orderId; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
    public void setCustomerName(String customerName)     { this.customerName   = customerName; }
    public void setOrderAmount(double orderAmount)       { this.orderAmount    = orderAmount; }
    public void setTipAmount(double tipAmount)           { this.tipAmount      = tipAmount; }
    public void setCommission(double commission)         { this.commission     = commission; }
    public void setTotalEarning(double totalEarning)     { this.totalEarning   = totalEarning; }
    public void setTimestamp(long timestamp)             { this.timestamp      = timestamp; }
    public void setDate(String date)                     { this.date           = date; }

    public String getShortOrderId() {
        if (orderId == null || orderId.isEmpty()) return "#--------";
        return "#" + (orderId.length() > 8
                ? orderId.substring(orderId.length() - 8).toUpperCase()
                : orderId.toUpperCase());
    }
}