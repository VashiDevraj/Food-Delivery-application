package com.example.fooddeliveryapp.utils;

public class Constants {

    // ── SharedPreferences ─────────────────────────────────────────────────────
    public static final String PREF_NAME        = "food_app_session";
    public static final String PREF_USER_ID     = "user_id";
    public static final String PREF_USER_NAME   = "user_name";
    public static final String PREF_USER_ROLE   = "role";
    public static final String PREF_IS_LOGGED   = "isLoggedIn";

    // ── Firebase: Top-Level Nodes ─────────────────────────────────────────────
    public static final String NODE_USERS         = "Users";
    public static final String NODE_ADMINS        = "Admins";
    public static final String NODE_ORDERS        = "Orders";
    public static final String NODE_FOOD_ITEMS    = "FoodItems";
    public static final String NODE_CATEGORIES    = "Categories";
    public static final String NODE_RESTAURANTS   = "Restaurants";
    public static final String NODE_DELIVERY_BOYS = "DeliveryBoys";

    // ── Firebase: Sub-Nodes ───────────────────────────────────────────────────
    public static final String NODE_PROFILE         = "profile";
    public static final String NODE_CART            = "Cart";
    public static final String NODE_MY_ORDERS       = "Orders";
    public static final String NODE_RESTAURANT      = "Restaurant";
    public static final String NODE_RESTAURANT_INFO = "info";
    public static final String NODE_MENU            = "Menu";
    public static final String NODE_EARNING_HISTORY = "EarningHistory";
    public static final String NODE_ASSIGNED_ORDERS = "AssignedOrders";
    public static final String NODE_RATINGS         = "Ratings";        // Delivery boy ratings

    // ── User Roles ────────────────────────────────────────────────────────────
    public static final String ROLE_ADMIN        = "admin";
    public static final String ROLE_USER         = "user";
    public static final String ROLE_DELIVERY_BOY = "delivery_boy";

    // ── Order Status ──────────────────────────────────────────────────────────
    // ── Order Status (FIXED — ALL CAPS, NO SPACES) ──
    public static final String STATUS_PENDING          = "PENDING";
    public static final String STATUS_PREPARING        = "PREPARING";
    public static final String STATUS_PICKED_UP        = "PICKED_UP";
    public static final String STATUS_OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
    public static final String STATUS_DELIVERED        = "DELIVERED";
    public static final String STATUS_CANCELLED        = "CANCELLED";

    // ── Delivery Boy Internal Status ──────────────────────────────────────────
    public static final String DELIVERY_STATUS_PENDING  = "PENDING";
    public static final String DELIVERY_STATUS_ACCEPTED = "ACCEPTED";
    public static final String DELIVERY_STATUS_REJECTED = "REJECTED";

    // ── Payment Status ────────────────────────────────────────────────────────
    public static final String PAYMENT_PENDING  = "Pending";
    public static final String PAYMENT_PAID     = "Paid";
    public static final String PAYMENT_REFUNDED = "Refunded";

    // ── Intent Extras ─────────────────────────────────────────────────────────
    public static final String EXTRA_FOOD_ID         = "foodId";
    public static final String EXTRA_CATEGORY_ID     = "categoryId";
    public static final String EXTRA_ORDER_ID        = "orderId";
    public static final String EXTRA_FOOD_ITEM       = "foodItem";
    public static final String EXTRA_RESTAURANT_ID   = "restaurantId";
    public static final String EXTRA_RESTAURANT_NAME = "restaurantName";
    public static final String EXTRA_DELIVERY_BOY_ID = "deliveryBoyId";
    public static final String EXTRA_TIP_AMOUNT      = "tipAmount";

    // ── Delivery Commission ───────────────────────────────────────────────────
    public static final double DELIVERY_COMMISSION_PERCENT = 10.0;  // 10% of order value
    public static final double BASE_DELIVERY_FEE            = 30.0; // ₹30 base per delivery

    // ── Misc ──────────────────────────────────────────────────────────────────
    public static final int SPLASH_DELAY             = 2000;
    public static final int ORDER_ACCEPT_TIMEOUT_MS  = 60_000; // 60 seconds to accept/reject
    public static final int MAX_REVIEW_IMAGE_PX      = 800;    // max pixel for review photo

    // ── Firebase DB URL ───────────────────────────────────────────────────────
    public static final String FIREBASE_URL =
            "https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app";

    // ── Payments ──────────────────────────────────────────────────────────────
    public static final String UPI_VPA        = "vashidevraj50@oksbi";
    public static final String FCM_SERVER_KEY = "YOUR_SERVER_KEY_HERE";

    // ═════════════════════════════════════════════════════════════════════════
    //  Path helpers
    // ═════════════════════════════════════════════════════════════════════════

    public static String userProfilePath(String userId) {
        return NODE_USERS + "/" + userId + "/" + NODE_PROFILE;
    }

    public static String userCartItemPath(String userId, String foodId) {
        return NODE_USERS + "/" + userId + "/" + NODE_CART + "/" + foodId;
    }

    public static String userCartPath(String userId) {
        return NODE_USERS + "/" + userId + "/" + NODE_CART;
    }

    public static String userOrdersPath(String userId) {
        return NODE_USERS + "/" + userId + "/" + NODE_MY_ORDERS;
    }

    public static String adminProfilePath(String adminId) {
        return NODE_ADMINS + "/" + adminId + "/" + NODE_PROFILE;
    }

    public static String adminRestaurantInfoPath(String adminId) {
        return NODE_ADMINS + "/" + adminId + "/" + NODE_RESTAURANT + "/" + NODE_RESTAURANT_INFO;
    }

    public static String adminMenuPath(String adminId) {
        return NODE_ADMINS + "/" + adminId + "/" + NODE_RESTAURANT + "/" + NODE_MENU;
    }

    public static String globalOrderPath(String orderId) {
        return NODE_ORDERS + "/" + orderId;
    }

    public static String deliveryBoyProfilePath(String uid) {
        return NODE_DELIVERY_BOYS + "/" + uid + "/" + NODE_PROFILE;
    }

    public static String deliveryBoyEarningsPath(String uid) {
        return NODE_DELIVERY_BOYS + "/" + uid + "/" + NODE_EARNING_HISTORY;
    }

    public static String deliveryBoyAssignedOrdersPath(String uid) {
        return NODE_DELIVERY_BOYS + "/" + uid + "/" + NODE_ASSIGNED_ORDERS;
    }

    public static String deliveryBoyRatingsPath(String uid) {
        return NODE_DELIVERY_BOYS + "/" + uid + "/" + NODE_RATINGS;
    }
}