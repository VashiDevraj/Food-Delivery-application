package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.adapters.FavFoodAdapter;
import com.example.fooddeliveryapp.adapters.FavRestaurantAdapter;
import com.example.fooddeliveryapp.models.FoodItem;
import com.example.fooddeliveryapp.models.Restaurant;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavouritesActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DatabaseReference dbRef;

    private RecyclerView rvFavRestaurants, rvFavFoods;
    private LinearLayout layoutEmptyFavs, layoutRestaurantSection, layoutFoodSection;
    private TextView tvRestaurantCount, tvFoodCount;

    private final List<Restaurant> favRestaurants = new ArrayList<>();
    private final List<FoodItem>   favFoods       = new ArrayList<>();

    private FavRestaurantAdapter restaurantAdapter;
    private FavFoodAdapter       foodAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        sessionManager = new SessionManager(this);
        dbRef = FirebaseDatabase
                .getInstance("https://fooddeliveryapp-f65dc-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference();

        rvFavRestaurants    = findViewById(R.id.rvFavRestaurants);
        rvFavFoods          = findViewById(R.id.rvFavFoods);
        layoutEmptyFavs     = findViewById(R.id.layoutEmptyFavs);
        layoutRestaurantSection = findViewById(R.id.layoutRestaurantSection);
        layoutFoodSection   = findViewById(R.id.layoutFoodSection);
        tvRestaurantCount   = findViewById(R.id.tvFavRestaurantCount);
        tvFoodCount         = findViewById(R.id.tvFavFoodCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        restaurantAdapter = new FavRestaurantAdapter(this, favRestaurants,
                restaurant -> {
                    Intent intent = new Intent(this, RestaurantMenuActivity.class);
                    intent.putExtra(Constants.EXTRA_RESTAURANT_ID,   restaurant.getRestaurantId());
                    intent.putExtra(Constants.EXTRA_RESTAURANT_NAME, restaurant.getName());
                    startActivity(intent);
                },
                restaurant -> {
                    removeFavRestaurant(restaurant.getRestaurantId());
                });

        foodAdapter = new FavFoodAdapter(this, favFoods,
                food -> {
                    String rid = food.getRestaurantId();
                    if (rid != null && !rid.isEmpty()) {
                        Intent intent = new Intent(this, RestaurantMenuActivity.class);
                        intent.putExtra(Constants.EXTRA_RESTAURANT_ID, rid);
                        startActivity(intent);
                    }
                },
                food -> removeFavFood(food.getFoodId()));

        rvFavRestaurants.setLayoutManager(new LinearLayoutManager(this));
        rvFavRestaurants.setAdapter(restaurantAdapter);

        rvFavFoods.setLayoutManager(new LinearLayoutManager(this));
        rvFavFoods.setAdapter(foodAdapter);

        loadFavourites();
    }

    private void loadFavourites() {
        String uid = sessionManager.getUid();
        if (uid == null) return;

        // Load favourite restaurants
        dbRef.child(Constants.NODE_USERS).child(uid).child("favourites").child("restaurants")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favRestaurants.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String rid  = snap.child("restaurantId").getValue(String.class);
                            String name = snap.child("name").getValue(String.class);
                            Double rating = snap.child("rating").getValue(Double.class);
                            if (rid != null) {
                                Restaurant r = new Restaurant();
                                r.setRestaurantId(rid);
                                r.setName(name != null ? name : "Restaurant");
                                if (rating != null) r.setRating(rating.floatValue());
                                // Fetch full image from Restaurants node
                                fetchRestaurantImage(r);
                                favRestaurants.add(r);
                            }
                        }
                        tvRestaurantCount.setText(favRestaurants.size() + " saved");
                        layoutRestaurantSection.setVisibility(
                                favRestaurants.isEmpty() ? View.GONE : View.VISIBLE);
                        restaurantAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // Load favourite foods
        dbRef.child(Constants.NODE_USERS).child(uid).child("favourites").child("foods")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        favFoods.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String foodId = snap.child("foodId").getValue(String.class);
                            String name   = snap.child("name").getValue(String.class);
                            Double price  = snap.child("price").getValue(Double.class);
                            String rid    = snap.child("restaurantId").getValue(String.class);
                            if (foodId != null) {
                                FoodItem f = new FoodItem();
                                f.setFoodId(foodId);
                                f.setName(name != null ? name : "Food");
                                if (price != null) f.setPrice(price.doubleValue());
                                f.setRestaurantId(rid);
                                // Fetch full image from FoodItems node
                                fetchFoodImage(f);
                                favFoods.add(f);
                            }
                        }
                        tvFoodCount.setText(favFoods.size() + " saved");
                        layoutFoodSection.setVisibility(
                                favFoods.isEmpty() ? View.GONE : View.VISIBLE);
                        foodAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void fetchRestaurantImage(Restaurant r) {
        dbRef.child(Constants.NODE_RESTAURANTS).child(r.getRestaurantId())
                .child("imageBase64")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String img = snapshot.getValue(String.class);
                        if (img != null && !img.isEmpty()) {
                            r.setImageBase64(img);
                            restaurantAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void fetchFoodImage(FoodItem f) {
        dbRef.child(Constants.NODE_FOOD_ITEMS).child(f.getFoodId())
                .child("imageBase64")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String img = snapshot.getValue(String.class);
                        if (img != null && !img.isEmpty()) {
                            f.setImageBase64(img);
                            foodAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void removeFavRestaurant(String restaurantId) {
        String uid = sessionManager.getUid();
        if (uid == null) return;
        dbRef.child(Constants.NODE_USERS).child(uid)
                .child("favourites").child("restaurants").child(restaurantId)
                .removeValue()
                .addOnSuccessListener(u ->
                        Toast.makeText(this, "Removed from favourites", Toast.LENGTH_SHORT).show());
    }

    private void removeFavFood(String foodId) {
        String uid = sessionManager.getUid();
        if (uid == null) return;
        dbRef.child(Constants.NODE_USERS).child(uid)
                .child("favourites").child("foods").child(foodId)
                .removeValue()
                .addOnSuccessListener(u ->
                        Toast.makeText(this, "Removed from favourites", Toast.LENGTH_SHORT).show());
    }

    private void updateEmptyState() {
        boolean isEmpty = favRestaurants.isEmpty() && favFoods.isEmpty();
        layoutEmptyFavs.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}