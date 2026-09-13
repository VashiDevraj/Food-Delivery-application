package com.example.fooddeliveryapp.models;

/**
 * CartItem — stored at:  Users/{userId}/Cart/{foodId}
 *
 * The node key IS the foodId, so foodId is stored as a field too
 * for convenience when reading the list back.
 */
public class CartItem {

    private String foodId;
    private String name;
    private double price;       // unit price
    private int    quantity;
    private double totalPrice;  // price * quantity

    // Required empty constructor for Firebase deserialization
    public CartItem() {}

    public CartItem(String foodId, String name, double price,
                    int quantity, double totalPrice) {
        this.foodId     = foodId;
        this.name       = name;
        this.price      = price;
        this.quantity   = quantity;
        this.totalPrice = totalPrice;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getFoodId()     { return foodId; }
    public String getName()       { return name   != null ? name : ""; }
    public double getPrice()      { return price; }
    public int    getQuantity()   { return quantity; }
    public double getTotalPrice() { return totalPrice; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setFoodId(String foodId)        { this.foodId     = foodId; }
    public void setName(String name)            { this.name       = name; }
    public void setPrice(double price)          { this.price      = price; }
    public void setQuantity(int quantity)       { this.quantity   = quantity; }
    public void setTotalPrice(double totalPrice){ this.totalPrice = totalPrice; }
}