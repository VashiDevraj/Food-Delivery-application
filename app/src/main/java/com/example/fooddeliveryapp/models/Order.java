package com.example.fooddeliveryapp.models;

import com.example.fooddeliveryapp.utils.Constants;

import java.util.Map;

/**
 * Order — written to TWO places simultaneously:
 *   1. Orders/{orderId}                    ← global; admin reads from here
 *   2. Users/{userId}/Orders/{orderId}     ← user's own history
 *
 * Also mirrored to:
 *   3. DeliveryBoys/{dbUid}/AssignedOrders/{orderId}  ← delivery boy's view
 */
public class Order {

    private String  orderId;
    private String  cancelReason;
    private String  cancelledBy;
    private String deliveryStatus; // ASSIGNED / ACCEPTED / REJECTED / PENDING
    private String  userId;
    private String  userName;
    private String  restaurantId;
    private String  restaurantName;
    private double  totalAmount;
    private String  status;
    private long    timestamp;
    private Map<String, CartItem> items;
    private String  paymentMethod;
    private String  paymentStatus;
    private String  address;
    private String  phone;
    private String  notes;
    private boolean reviewed;

    // ── NEW delivery boy fields ───────────────────────────────────────────────
    private String  deliveryBoyId;      // uid of assigned delivery boy
    private String  deliveryBoyName;    // cached name for display
    private double  tipAmount;          // tip given by user (₹10/20/50/custom)
    private String  deliveryNote;       // user's note to delivery boy
    private boolean codCollected;       // true once delivery boy confirms COD collection

    public Order() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getOrderId()         { return orderId; }
    public String  getUserId()          { return userId; }
    public String  getCancelReason()    { return cancelReason; }
    public String getDeliveryStatus() {
        return deliveryStatus != null ? deliveryStatus : "PENDING";
    }
    public String  getCancelledBy()     { return cancelledBy; }
    public String  getUserName()        { return userName       != null ? userName       : ""; }
    public String  getRestaurantId()    { return restaurantId   != null ? restaurantId   : ""; }
    public String  getRestaurantName()  { return restaurantName != null ? restaurantName : ""; }
    public double  getTotalAmount()     { return totalAmount; }
    public String getStatus() {
        return status != null ? status : Constants.STATUS_PENDING;
    }
    public long    getTimestamp()       { return timestamp; }
    public Map<String, CartItem> getItems() { return items; }
    public String  getPaymentMethod()   { return paymentMethod  != null ? paymentMethod  : "N/A"; }
    public String  getPaymentStatus()   { return paymentStatus  != null ? paymentStatus  : "N/A"; }
    public String  getAddress()         { return address        != null ? address        : "N/A"; }
    public String  getPhone()           { return phone          != null ? phone          : "N/A"; }
    public String  getNotes()           { return notes; }
    public boolean isReviewed()         { return reviewed; }
    public String  getDeliveryBoyId()   { return deliveryBoyId  != null ? deliveryBoyId  : ""; }
    public String  getDeliveryBoyName() { return deliveryBoyName != null ? deliveryBoyName : ""; }
    public double  getTipAmount()       { return tipAmount; }
    public String  getDeliveryNote()    { return deliveryNote   != null ? deliveryNote   : ""; }
    public boolean isCodCollected()     { return codCollected; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setOrderId(String orderId)                { this.orderId        = orderId; }
    public void setUserId(String userId)                  { this.userId         = userId; }
    public void setCancelReason(String cancelReason)      { this.cancelReason   = cancelReason; }
    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
    public void setCancelledBy(String cancelledBy)        { this.cancelledBy    = cancelledBy; }
    public void setUserName(String userName)              { this.userName       = userName; }
    public void setRestaurantId(String restaurantId)      { this.restaurantId   = restaurantId; }
    public void setRestaurantName(String restaurantName)  { this.restaurantName = restaurantName; }
    public void setTotalAmount(double totalAmount)        { this.totalAmount    = totalAmount; }
    public void setStatus(String status)                  { this.status         = status; }
    public void setTimestamp(long timestamp)              { this.timestamp      = timestamp; }
    public void setItems(Map<String, CartItem> items)     { this.items          = items; }
    public void setPaymentMethod(String paymentMethod)    { this.paymentMethod  = paymentMethod; }
    public void setPaymentStatus(String paymentStatus)    { this.paymentStatus  = paymentStatus; }
    public void setAddress(String address)                { this.address        = address; }
    public void setPhone(String phone)                    { this.phone          = phone; }
    public void setNotes(String notes)                    { this.notes          = notes; }
    public void setReviewed(boolean reviewed)             { this.reviewed       = reviewed; }
    public void setDeliveryBoyId(String deliveryBoyId)    { this.deliveryBoyId  = deliveryBoyId; }
    public void setDeliveryBoyName(String name)           { this.deliveryBoyName = name; }
    public void setTipAmount(double tipAmount)            { this.tipAmount      = tipAmount; }
    public void setDeliveryNote(String deliveryNote)      { this.deliveryNote   = deliveryNote; }
    public void setCodCollected(boolean codCollected)     { this.codCollected   = codCollected; }

    // ── Utility ───────────────────────────────────────────────────────────────
    public String getShortOrderId() {
        if (orderId == null || orderId.isEmpty()) return "#--------";
        return "#" + (orderId.length() > 8
                ? orderId.substring(orderId.length() - 8).toUpperCase()
                : orderId.toUpperCase());
    }

    public int getItemCount() {
        if (items == null) return 0;
        int count = 0;
        for (CartItem item : items.values()) count += item.getQuantity();
        return count;
    }

    public boolean isCOD() {
        return "COD".equalsIgnoreCase(paymentMethod);
    }

    public boolean isAssigned() {
        return deliveryBoyId != null && !deliveryBoyId.isEmpty();
    }
}