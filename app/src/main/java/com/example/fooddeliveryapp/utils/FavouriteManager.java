package com.example.fooddeliveryapp.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.fooddeliveryapp.models.FoodItem;
import com.example.fooddeliveryapp.models.Restaurant;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages favourites state:
 * - Maintains an in-memory cache so heart icons are instant.
 * - Syncs adds/removes to Firebase Realtime Database.
 * - Preloads the user's existing favourites on construction.
 */
public class FavouriteManager {

    private static final Set<String>  favRestaurantIds = new HashSet<>();
    private static final Set<String>  favFoodIds       = new HashSet<>();
    private static boolean            loaded           = false;

    private final DatabaseReference dbRef;
    private final String            uid;

    public FavouriteManager(Context context) {
        SessionManager session = new SessionManager(context);
        uid = session.getUid();
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();
        if (!loaded && uid != null) {
            preload();
        }
    }

    private void preload() {
        loaded = true;
        dbRef.child(Constants.NODE_USERS).child(uid).child("favourites").child("restaurants")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favRestaurantIds.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String rid = snap.child("restaurantId").getValue(String.class);
                            if (rid != null) favRestaurantIds.add(rid);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        dbRef.child(Constants.NODE_USERS).child(uid).child("favourites").child("foods")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favFoodIds.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String fid = snap.child("foodId").getValue(String.class);
                            if (fid != null) favFoodIds.add(fid);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public boolean isRestaurantFavourite(String restaurantId) {
        return restaurantId != null && favRestaurantIds.contains(restaurantId);
    }

    public boolean isFoodFavourite(String foodId) {
        return foodId != null && favFoodIds.contains(foodId);
    }

    // ── Toggle Restaurant ─────────────────────────────────────────────────────

    public void setRestaurantFavourite(Restaurant restaurant, boolean fav) {
        if (uid == null || restaurant.getRestaurantId() == null) return;
        DatabaseReference ref = dbRef.child(Constants.NODE_USERS).child(uid)
                .child("favourites").child("restaurants")
                .child(restaurant.getRestaurantId());
        if (fav) {
            favRestaurantIds.add(restaurant.getRestaurantId());
            Map<String, Object> data = new HashMap<>();
            data.put("restaurantId", restaurant.getRestaurantId());
            data.put("name",         restaurant.getName());
            data.put("rating",       restaurant.getRating());
            data.put("type",         "restaurant");
            ref.setValue(data);
        } else {
            favRestaurantIds.remove(restaurant.getRestaurantId());
            ref.removeValue();
        }
    }

    // ── Toggle Food ───────────────────────────────────────────────────────────

    public void setFoodFavourite(FoodItem food, boolean fav) {
        if (uid == null || food.getFoodId() == null) return;
        DatabaseReference ref = dbRef.child(Constants.NODE_USERS).child(uid)
                .child("favourites").child("foods")
                .child(food.getFoodId());
        if (fav) {
            favFoodIds.add(food.getFoodId());
            Map<String, Object> data = new HashMap<>();
            data.put("foodId",       food.getFoodId());
            data.put("name",         food.getName());
            data.put("price",        food.getPrice());
            data.put("restaurantId", food.getRestaurantId());
            data.put("type",         "food");
            ref.setValue(data);
        } else {
            favFoodIds.remove(food.getFoodId());
            ref.removeValue();
        }
    }

    /** Call when user logs out to clear the static cache */
    public static void clearCache() {
        favRestaurantIds.clear();
        favFoodIds.clear();
        loaded = false;
    }
}