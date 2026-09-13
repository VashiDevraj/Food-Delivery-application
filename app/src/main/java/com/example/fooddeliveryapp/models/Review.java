package com.example.fooddeliveryapp.models;

/**
 * Review — stored at: Reviews/{restaurantId}/{reviewId}
 *
 * FIXED:
 * ✅ Added deliveryBoyId, deliveryBoyName, ratingDeliveryBoy fields
 * ✅ Added reviewImageBase64 field
 * ✅ ratingOverall excludes deliveryBoy rating (restaurant-only avg)
 * ✅ Full getters/setters for all fields
 */
public class Review {

    private String reviewId;
    private String orderId;
    private String restaurantId;
    private String userId;
    private String userName;

    // Restaurant sub-ratings (1–5 each)
    private float ratingFood;           // Food quality
    private float ratingDelivery;       // Delivery experience
    private float ratingPacking;        // Packing quality
    private float ratingOverall;        // (ratingFood + ratingDelivery + ratingPacking) / 3

    // Delivery boy specific rating
    private float  ratingDeliveryBoy;   // Separate rating for the delivery partner
    private String deliveryBoyId;       // uid of delivery boy rated
    private String deliveryBoyName;     // cached name for display

    private String comment;
    private String reviewImageBase64;   // Base64 encoded photo (may be empty)
    private long   timestamp;

    public Review() {}

    public Review(String reviewId, String orderId, String restaurantId,
                  String userId, String userName,
                  float ratingFood, float ratingDelivery, float ratingPacking,
                  float ratingDeliveryBoy, String deliveryBoyId, String deliveryBoyName,
                  String comment, String reviewImageBase64, long timestamp) {
        this.reviewId          = reviewId;
        this.orderId           = orderId;
        this.restaurantId      = restaurantId;
        this.userId            = userId;
        this.userName          = userName;
        this.ratingFood        = ratingFood;
        this.ratingDelivery    = ratingDelivery;
        this.ratingPacking     = ratingPacking;
        this.ratingOverall     = (ratingFood + ratingDelivery + ratingPacking) / 3f;
        this.ratingDeliveryBoy = ratingDeliveryBoy;
        this.deliveryBoyId     = deliveryBoyId;
        this.deliveryBoyName   = deliveryBoyName;
        this.comment           = comment;
        this.reviewImageBase64 = reviewImageBase64;
        this.timestamp         = timestamp;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getReviewId()         { return reviewId; }
    public String getOrderId()          { return orderId; }
    public String getRestaurantId()     { return restaurantId; }
    public String getUserId()           { return userId; }
    public String getUserName()         { return userName        != null ? userName        : "Anonymous"; }
    public float  getRatingFood()       { return ratingFood; }
    public float  getRatingDelivery()   { return ratingDelivery; }
    public float  getRatingPacking()    { return ratingPacking; }
    public float  getRatingOverall() {
        return ratingOverall > 0 ? ratingOverall
                : (ratingFood + ratingDelivery + ratingPacking) / 3f;
    }
    public float  getRatingDeliveryBoy(){ return ratingDeliveryBoy; }
    public String getDeliveryBoyId()    { return deliveryBoyId   != null ? deliveryBoyId   : ""; }
    public String getDeliveryBoyName()  { return deliveryBoyName != null ? deliveryBoyName : ""; }
    public String getComment()          { return comment         != null ? comment         : ""; }
    public String getReviewImageBase64(){ return reviewImageBase64 != null ? reviewImageBase64 : ""; }
    public long   getTimestamp()        { return timestamp; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setReviewId(String reviewId)                { this.reviewId          = reviewId; }
    public void setOrderId(String orderId)                  { this.orderId           = orderId; }
    public void setRestaurantId(String restaurantId)        { this.restaurantId      = restaurantId; }
    public void setUserId(String userId)                    { this.userId            = userId; }
    public void setUserName(String userName)                { this.userName          = userName; }
    public void setRatingFood(float r)                      { this.ratingFood        = r; }
    public void setRatingDelivery(float r)                  { this.ratingDelivery    = r; }
    public void setRatingPacking(float r)                   { this.ratingPacking     = r; }
    public void setRatingOverall(float r)                   { this.ratingOverall     = r; }
    public void setRatingDeliveryBoy(float r)               { this.ratingDeliveryBoy = r; }
    public void setDeliveryBoyId(String deliveryBoyId)      { this.deliveryBoyId     = deliveryBoyId; }
    public void setDeliveryBoyName(String deliveryBoyName)  { this.deliveryBoyName   = deliveryBoyName; }
    public void setComment(String comment)                  { this.comment           = comment; }
    public void setReviewImageBase64(String b64)            { this.reviewImageBase64 = b64; }
    public void setTimestamp(long timestamp)                { this.timestamp         = timestamp; }

    // ── Utility ───────────────────────────────────────────────────────────────
    public boolean hasDeliveryBoyRating() {
        return deliveryBoyId != null && !deliveryBoyId.isEmpty() && ratingDeliveryBoy > 0;
    }

    public boolean hasPhoto() {
        return reviewImageBase64 != null && !reviewImageBase64.isEmpty();
    }

    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "dd MMM yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
}