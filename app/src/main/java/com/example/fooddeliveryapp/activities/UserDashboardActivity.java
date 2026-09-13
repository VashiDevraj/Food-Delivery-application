package com.example.fooddeliveryapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.adapters.CategoryAdapter;
import com.example.fooddeliveryapp.adapters.FoodAdapter;
import com.example.fooddeliveryapp.adapters.OfferBannerAdapter;
import com.example.fooddeliveryapp.adapters.RestaurantAdapter;
import com.example.fooddeliveryapp.adapters.SpotlightAdapter;
import com.example.fooddeliveryapp.adapters.SuggestionAdapter;
import com.example.fooddeliveryapp.databinding.ActivityUserDashboardBinding;
import com.example.fooddeliveryapp.models.Category;
import com.example.fooddeliveryapp.models.FoodItem;
import com.example.fooddeliveryapp.models.OfferBanner;
import com.example.fooddeliveryapp.models.Restaurant;
import com.example.fooddeliveryapp.utils.Constants;
import com.example.fooddeliveryapp.utils.RestaurantStatusHelper;
import com.example.fooddeliveryapp.utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDashboardActivity extends AppCompatActivity {

    private ActivityUserDashboardBinding binding;
    private SessionManager               sessionManager;
    private DatabaseReference            dbRef;

    // ── Adapters ──────────────────────────────────────────────────────────────
    private OfferBannerAdapter    offerBannerAdapter;
    private CategoryAdapter       categoryAdapter;
    private RestaurantAdapter     recommendedAdapter;
    private SpotlightAdapter      spotlightAdapter;
    private RestaurantAdapter     allRestaurantsAdapter;
    private FoodAdapter           foodAdapter;
    private SuggestionAdapter     suggestionAdapter;

    // ── Data lists ────────────────────────────────────────────────────────────
    private final List<OfferBanner> offerBannerList    = new ArrayList<>();
    private final List<Category>    categoryList       = new ArrayList<>();
    private final List<Restaurant>  recommendedList    = new ArrayList<>();
    private final List<Restaurant>  spotlightList      = new ArrayList<>();
    private final List<Restaurant>  allRestaurants     = new ArrayList<>();
    private final List<Restaurant>  allRestaurantsFull = new ArrayList<>();
    private final List<FoodItem>    foodList           = new ArrayList<>();
    private final List<FoodItem>    allFoodList        = new ArrayList<>();
    private final List<String>      suggestions        = new ArrayList<>();

    // ── Auto-scroll ───────────────────────────────────────────────────────────
    private final Handler bannerHandler     = new Handler(Looper.getMainLooper());
    private int           currentBannerPage = 0;

    // ── Filters ───────────────────────────────────────────────────────────────
    private boolean isVegMode      = false;
    private boolean filterUnder250 = false;
    private boolean filterRating   = false;
    private boolean filterOffers   = false;
    private String  activeCategory = "all";

    private static final String[] CATEGORY_EMOJIS = {
            "🍕","🍔","🍟","🍜","🍛","🥗","🍱","🍣",
            "🍦","☕","🥞","🌮","🥙","🍢","🥘","🍲"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        dbRef = FirebaseDatabase
                .getInstance(Constants.FIREBASE_URL)
                .getReference();
        binding.cardCartBar.setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
        });

        isVegMode = sessionManager.isVegMode();

        setupUserAvatar();
        setupVegModeToggle();
        setupOfferBanners();
        setupCategoryRecyclerView();
        setupRecommendedRecyclerView();
        setupSpotlightRecyclerView();
        setupAllRestaurantsRecyclerView();
        setupFoodRecyclerView();
        setupSuggestions();
        setupSearch();
        setupFilterChips();
        setupExploreButtons();
        setupBottomNavigation();

        loadAddressFromFirebase();
        loadCategories();
        loadRestaurants();
        loadFoodItems();
        loadOfferBanners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddressFromFirebase();
        isVegMode = sessionManager.isVegMode();
        binding.switchVegMode.setChecked(isVegMode);
        if (foodAdapter != null) foodAdapter.notifyDataSetChanged();
        if (allRestaurantsAdapter != null) allRestaurantsAdapter.notifyDataSetChanged();
        updateCartBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bannerHandler.removeCallbacksAndMessages(null);
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    private void setupUserAvatar() {
        String name = sessionManager.getName();
        if (name != null && !name.isEmpty())
            binding.tvUserAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        binding.cvUserAvatar.setOnClickListener(v ->
                startActivity(new Intent(this, UserProfileActivity.class)));
    }

    // ── Veg mode ──────────────────────────────────────────────────────────────

    private void setupVegModeToggle() {
        binding.switchVegMode.setChecked(isVegMode);
        binding.switchVegMode.setOnCheckedChangeListener((btn, checked) -> {
            isVegMode = checked;
            sessionManager.setVegMode(checked);
            Toast.makeText(this, checked ? "Veg Mode ON 🥦" : "Veg Mode OFF",
                    Toast.LENGTH_SHORT).show();
            applyAllFilters();
        });
    }

    // ── Offer Banners (replaces hotel photo banners) ──────────────────────────

    private void setupOfferBanners() {
        offerBannerAdapter = new OfferBannerAdapter(this, offerBannerList, banner -> {
            // If a restaurant is linked, open it; otherwise apply an offer filter
            if (!banner.getLinkedRestaurantId().isEmpty()) {
                for (Restaurant r : allRestaurantsFull) {
                    if (r.getRestaurantId().equals(banner.getLinkedRestaurantId())) {
                        openRestaurant(r);
                        return;
                    }
                }
            }
            // Default: highlight offer deals
            filterOffers = true;
            binding.chipGreatOffers.setChecked(true);
            applyAllFilters();
        });

        binding.vpOfferBanners.setAdapter(offerBannerAdapter);
        binding.vpOfferBanners.setOffscreenPageLimit(2);

        // Add page transformer for a card-peek effect
        float pageMarginPx = getResources().getDimensionPixelOffset(R.dimen.vp_page_margin);
        float offsetPx     = getResources().getDimensionPixelOffset(R.dimen.vp_offset);
        binding.vpOfferBanners.setPageTransformer((page, position) -> {
            page.setTranslationX(-pageMarginPx * 2 * position);
            page.setScaleY(1 - (0.06f * Math.abs(position)));
            page.setAlpha(0.75f + (1 - Math.abs(position)) * 0.25f);
        });

        binding.vpOfferBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                currentBannerPage = position;
                updateBannerDots(position);
            }
        });
    }

    private void loadOfferBanners() {
        dbRef.child("offerBanners")
                .orderByChild("sortOrder")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        offerBannerList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            OfferBanner b = snap.getValue(OfferBanner.class);
                            if (b != null && b.isActive()) offerBannerList.add(b);
                        }
                        offerBannerAdapter.notifyDataSetChanged();
                        // Use fallback count if no admin banners
                        int count = offerBannerList.isEmpty() ? 5 : offerBannerList.size();
                        setupBannerDots(count);
                        startBannerAutoScroll();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        // Show 5 fallback banners
                        offerBannerList.clear();
                        offerBannerAdapter.notifyDataSetChanged();
                        setupBannerDots(5);
                        startBannerAutoScroll();
                    }
                });
    }

    private void startBannerAutoScroll() {
        int total = offerBannerList.isEmpty() ? 5 : offerBannerList.size();
        if (total == 0) return;
        bannerHandler.removeCallbacksAndMessages(null);
        bannerHandler.postDelayed(new Runnable() {
            @Override public void run() {
                int t = offerBannerList.isEmpty() ? 5 : offerBannerList.size();
                currentBannerPage = (currentBannerPage + 1) % t;
                binding.vpOfferBanners.setCurrentItem(currentBannerPage, true);
                bannerHandler.postDelayed(this, 3500);
            }
        }, 3500);
    }

    private void setupBannerDots(int count) {
        binding.layoutBannerDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(8, 8);
            p.setMargins(5, 0, 5, 0);
            dot.setLayoutParams(p);
            dot.setBackgroundResource(R.drawable.bg_dot_inactive);
            binding.layoutBannerDots.addView(dot);
        }
        updateBannerDots(0);
    }

    private void updateBannerDots(int idx) {
        for (int i = 0; i < binding.layoutBannerDots.getChildCount(); i++)
            binding.layoutBannerDots.getChildAt(i).setBackgroundResource(
                    i == idx ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
    }

    // ── Address ───────────────────────────────────────────────────────────────

    private void loadAddressFromFirebase() {
        String userId = sessionManager.getUid();
        if (userId == null) return;
        dbRef.child(Constants.NODE_USERS).child(userId).child(Constants.NODE_PROFILE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String city    = snapshot.child("city").getValue(String.class);
                        String address = snapshot.child("address").getValue(String.class);
                        String pincode = snapshot.child("pincode").getValue(String.class);
                        if (city != null && !city.isEmpty())
                            sessionManager.saveUserAddress(city,
                                    address != null ? address : city,
                                    pincode != null ? pincode : "");
                        updateAddressBar(city, address);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        updateAddressBar(sessionManager.getUserCity(), sessionManager.getUserAddress());
                    }
                });
    }

    private void updateAddressBar(String city, String address) {
        if (city != null && !city.isEmpty()) {
            binding.tvCityName.setText(city + " ▾");
            binding.tvAddressShort.setText(
                    address != null && !address.isEmpty() ? address : "Tap to update");
        } else {
            binding.tvCityName.setText("Home ▾");
            binding.tvAddressShort.setText("Tap to add delivery address");
        }
        binding.tvCityName.setOnClickListener(v    -> showAddressBottomSheet());
        binding.tvAddressShort.setOnClickListener(v -> showAddressBottomSheet());
    }

    private void showAddressBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_address_form, null);
        dialog.setContentView(view);

        TextInputEditText etHouseNo  = view.findViewById(R.id.etHouseNo);
        TextInputEditText etStreet   = view.findViewById(R.id.etStreet);
        TextInputEditText etLandmark = view.findViewById(R.id.etLandmark);
        TextInputEditText etCity     = view.findViewById(R.id.etCity);
        TextInputEditText etPincode  = view.findViewById(R.id.etPincode);

        String savedCity = sessionManager.getUserCity();
        if (savedCity != null && !savedCity.isEmpty()) etCity.setText(savedCity);

        view.findViewById(R.id.btnSaveAddress).setOnClickListener(v -> {
            String city = et(etCity), pincode = et(etPincode);
            if (city.isEmpty() || pincode.isEmpty()) {
                Toast.makeText(this, "City and Pincode required", Toast.LENGTH_SHORT).show(); return;
            }
            if (pincode.length() != 6) {
                Toast.makeText(this, "Enter valid 6-digit pincode", Toast.LENGTH_SHORT).show(); return;
            }
            StringBuilder sb = new StringBuilder();
            String hn = et(etHouseNo), st = et(etStreet), lm = et(etLandmark);
            if (!hn.isEmpty()) sb.append(hn).append(", ");
            if (!st.isEmpty()) sb.append(st).append(", ");
            if (!lm.isEmpty()) sb.append(lm).append(", ");
            sb.append(city).append(" - ").append(pincode);
            String fa = sb.toString();
            sessionManager.saveUserAddress(city, fa, pincode);
            String uid = sessionManager.getUid();
            if (uid != null) {
                DatabaseReference profileRef = dbRef.child(Constants.NODE_USERS)
                        .child(uid).child(Constants.NODE_PROFILE);
                profileRef.child("city").setValue(city);
                profileRef.child("address").setValue(fa);
                profileRef.child("pincode").setValue(pincode);
            }
            updateAddressBar(city, fa);
            Toast.makeText(this, "Address saved ✅", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String et(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private void setupCategoryRecyclerView() {
        categoryAdapter = new CategoryAdapter(this, categoryList, category -> {
            activeCategory = category.getCategoryId();
            applyAllFilters();
        });
        binding.rvCategories.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupRecommendedRecyclerView() {
        recommendedAdapter = new RestaurantAdapter(this, recommendedList, this::openRestaurant);
        binding.rvRestaurants.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvRestaurants.setAdapter(recommendedAdapter);
    }

    private void setupSpotlightRecyclerView() {
        spotlightAdapter = new SpotlightAdapter(this, spotlightList, this::openRestaurant);
        binding.rvSpotlight.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvSpotlight.setAdapter(spotlightAdapter);
    }

    private void setupAllRestaurantsRecyclerView() {
        allRestaurantsAdapter = new RestaurantAdapter(this, allRestaurants, this::openRestaurant);
        binding.rvAllRestaurants.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAllRestaurants.setAdapter(allRestaurantsAdapter);
    }

    private void setupFoodRecyclerView() {
        foodAdapter = new FoodAdapter(this, foodList, new FoodAdapter.OnFoodClickListener() {
            @Override public void onFoodClick(FoodItem item) {
                String rid = item.getRestaurantId();
                if (rid != null && !rid.isEmpty()) {
                    // Check restaurant open status before navigating
                    Restaurant r = getRestaurantById(rid);
                    if (r != null && !RestaurantStatusHelper.isOpen(r)) {
                        showRestaurantClosedSnackbar(r);
                        return;
                    }
                    Intent intent = new Intent(UserDashboardActivity.this, RestaurantMenuActivity.class);
                    intent.putExtra(Constants.EXTRA_RESTAURANT_ID,   rid);
                    intent.putExtra(Constants.EXTRA_RESTAURANT_NAME, getRestaurantNameById(rid));
                    startActivity(intent);
                }
            }
            @Override public void onAddToCartClick(FoodItem item) {
                Restaurant r = getRestaurantById(item.getRestaurantId());
                if (r != null && !RestaurantStatusHelper.isOpen(r)) {
                    showRestaurantClosedSnackbar(r);
                    return;
                }
                addToCartFromDashboard(item);
            }
        });
        binding.rvFoodItems.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvFoodItems.setAdapter(foodAdapter);
    }

    // ── Closed restaurant feedback ─────────────────────────────────────────────

    private void showRestaurantClosedSnackbar(Restaurant r) {
        String msg = "🔴 " + r.getName() + " is closed · " + r.getOpensAtText();
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // ── Search with suggestions ───────────────────────────────────────────────

    private void setupSuggestions() {
        suggestionAdapter = new SuggestionAdapter(this, suggestions, query -> {
            binding.etSearch.setText(query);
            binding.rvSuggestions.setVisibility(View.GONE);
            filterRestaurantsByQuery(query.toLowerCase());
            filterFoodByQuery(query.toLowerCase());
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence query, int i, int b, int c) {
                String q = query.toString().trim().toLowerCase();
                if (q.isEmpty()) {
                    binding.rvSuggestions.setVisibility(View.GONE);
                    applyAllFilters();
                    return;
                }
                buildSuggestions(q);
                filterRestaurantsByQuery(q);
                filterFoodByQuery(q);
            }
        });
        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) binding.rvSuggestions.setVisibility(View.GONE);
        });
    }

    private void buildSuggestions(String q) {
        suggestions.clear();
        for (Restaurant r : allRestaurantsFull) {
            if (r.getName() != null && r.getName().toLowerCase().startsWith(q))
                if (!suggestions.contains(r.getName())) suggestions.add(r.getName());
        }
        for (FoodItem f : allFoodList) {
            if (f.getName() != null && f.getName().toLowerCase().startsWith(q))
                if (!suggestions.contains(f.getName())) suggestions.add(f.getName());
        }
        if (suggestions.size() > 6) suggestions.subList(6, suggestions.size()).clear();
        suggestionAdapter.notifyDataSetChanged();
        binding.rvSuggestions.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void filterRestaurantsByQuery(String q) {
        allRestaurants.clear();
        for (Restaurant r : allRestaurantsFull) {
            if (isVegMode && !r.isVegOnly()) continue;
            if (filterRating && r.getRating() < 4.0) continue;
            if (filterOffers && (r.getOffer() == null || r.getOffer().isEmpty())) continue;
            if (q.isEmpty()
                    || (r.getName() != null && r.getName().toLowerCase().contains(q))
                    || (r.getCity() != null && r.getCity().toLowerCase().contains(q)))
                allRestaurants.add(r);
        }
        allRestaurantsAdapter.notifyDataSetChanged();
        refreshRecommendedAndSpotlight();
        updateRestaurantCount();
    }

    private void filterFoodByQuery(String q) {
        foodList.clear();
        for (FoodItem item : allFoodList) {
            if (isVegMode && !item.isVeg()) continue;
            if (!item.isAvailable()) continue;
            if (item.getRestaurantId() == null || item.getRestaurantId().isEmpty()) continue;
            if (filterUnder250 && item.getPrice() > 250) continue;
            if (!"all".equals(activeCategory) && !activeCategory.equals(item.getCategoryId())) continue;
            if (q.isEmpty()
                    || (item.getName()        != null && item.getName().toLowerCase().contains(q))
                    || (item.getDescription() != null && item.getDescription().toLowerCase().contains(q)))
                foodList.add(item);
        }
        foodAdapter.notifyDataSetChanged();
    }

    private void updateRestaurantCount() {
        int c = allRestaurants.size();
        binding.tvRestaurantCount.setText(c + (c == 1 ? " place" : " places") + " delivering to you");
    }

    private void refreshRecommendedAndSpotlight() {
        recommendedList.clear(); spotlightList.clear();
        int count = 0;
        for (Restaurant r : allRestaurants) {
            if (count < 6) recommendedList.add(r);
            if (r.getRating() >= 4.0) spotlightList.add(r);
            count++;
        }
        recommendedAdapter.notifyDataSetChanged();
        spotlightAdapter.notifyDataSetChanged();
    }

    // ── Filter chips ──────────────────────────────────────────────────────────

    private void setupFilterChips() {
        binding.chipFilters.setOnClickListener(v -> showFilterOptions());
        binding.chipUnder250.setOnClickListener(v -> {
            filterUnder250 = !filterUnder250;
            binding.chipUnder250.setChecked(filterUnder250);
            applyAllFilters();
        });
        binding.chipGreatOffers.setOnClickListener(v -> {
            filterOffers = !filterOffers;
            binding.chipGreatOffers.setChecked(filterOffers);
            applyAllFilters();
        });
        binding.chipRating.setOnClickListener(v -> {
            filterRating = !filterRating;
            binding.chipRating.setChecked(filterRating);
            applyAllFilters();
        });
        binding.btnUnder250Promo.setOnClickListener(v -> {
            filterUnder250 = true;
            binding.chipUnder250.setChecked(true);
            applyAllFilters();
            binding.nestedScrollView.smoothScrollTo(0, 0);
        });
    }

    private void showFilterOptions() {
        String[] options  = {"Under ₹250", "4.0+ Rating", "Great Offers"};
        boolean[] checked = {filterUnder250, filterRating, filterOffers};
        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                .setTitle("⚙ Filters")
                .setMultiChoiceItems(options, checked, (d, which, isChecked) ->
                        checked[which] = isChecked)
                .setPositiveButton("Apply", (d, w) -> {
                    filterUnder250 = checked[0]; filterRating = checked[1]; filterOffers = checked[2];
                    binding.chipUnder250.setChecked(filterUnder250);
                    binding.chipRating.setChecked(filterRating);
                    binding.chipGreatOffers.setChecked(filterOffers);
                    applyAllFilters();
                })
                .setNegativeButton("Reset", (d, w) -> {
                    filterUnder250 = false; filterRating = false; filterOffers = false;
                    binding.chipUnder250.setChecked(false);
                    binding.chipRating.setChecked(false);
                    binding.chipGreatOffers.setChecked(false);
                    applyAllFilters();
                })
                .show();
    }

    private void applyAllFilters() {
        foodList.clear();
        for (FoodItem item : allFoodList) {
            if (isVegMode && !item.isVeg()) continue;
            if (!item.isAvailable()) continue;
            if (item.getRestaurantId() == null || item.getRestaurantId().isEmpty()) continue;
            if (filterUnder250 && item.getPrice() > 250) continue;
            if (!"all".equals(activeCategory) && !activeCategory.equals(item.getCategoryId())) continue;
            foodList.add(item);
        }
        foodAdapter.notifyDataSetChanged();

        allRestaurants.clear();
        for (Restaurant r : allRestaurantsFull) {
            if (isVegMode   && !r.isVegOnly()) continue;
            if (filterRating && r.getRating() < 4.0) continue;
            if (filterOffers && (r.getOffer() == null || r.getOffer().isEmpty())) continue;
            allRestaurants.add(r);
        }
        allRestaurantsAdapter.notifyDataSetChanged();
        refreshRecommendedAndSpotlight();
        updateRestaurantCount();
    }

    // ── Explore buttons ───────────────────────────────────────────────────────

    private void setupExploreButtons() {
        binding.llExploreOffers.setOnClickListener(v -> {
            filterOffers = true; binding.chipGreatOffers.setChecked(true);
            applyAllFilters(); scrollToRestaurants();
        });
        binding.llExploreTop10.setOnClickListener(v -> {
            filterRating = true; binding.chipRating.setChecked(true);
            applyAllFilters(); scrollToRestaurants();
        });
        binding.llExploreNew.setOnClickListener(v -> {
            resetAllFilters(); scrollToRestaurants();
        });
        binding.llExploreFavourites.setOnClickListener(v ->
                startActivity(new Intent(this, FavouritesActivity.class)));
    }

    private void scrollToRestaurants() {
        binding.nestedScrollView.post(() ->
                binding.nestedScrollView.smoothScrollTo(0, binding.rvAllRestaurants.getTop()));
    }

    private void resetAllFilters() {
        filterUnder250 = false; filterRating = false; filterOffers = false;
        activeCategory = "all";
        binding.chipUnder250.setChecked(false);
        binding.chipRating.setChecked(false);
        binding.chipGreatOffers.setChecked(false);
        binding.etSearch.setText("");
        applyAllFilters();
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                resetAllFilters();
                binding.nestedScrollView.smoothScrollTo(0, 0);
                return true;
            } else if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class)); return true;
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, OrderHistoryActivity.class)); return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class)); return true;
            }
            return false;
        });
    }

    // ── Cart bar (floating sticky) ────────────────────────────────────────────

    private void updateCartBar() {
        String userId = sessionManager.getUid();
        if (userId == null) return;
        dbRef.child(Constants.NODE_USERS).child(userId).child(Constants.NODE_CART)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        if (count > 0) {
                            binding.cardCartBar.setVisibility(View.VISIBLE);
                            binding.tvCartItemCount.setText(count + (count == 1 ? " item" : " items"));
                            // Fetch restaurant name for cart bar
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                String rid = snap.child("restaurantId").getValue(String.class);
                                if (rid != null) {
                                    binding.tvCartRestaurantName.setText(getRestaurantNameById(rid));
                                    break;
                                }
                            }
                        } else {
                            binding.cardCartBar.setVisibility(View.GONE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openRestaurant(Restaurant restaurant) {
        // Show closed overlay message if restaurant is closed
        if (!RestaurantStatusHelper.isOpen(restaurant)) {
            new androidx.appcompat.app.AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                    .setTitle("🔴 Restaurant Closed")
                    .setMessage(restaurant.getName() + " is currently closed.\n\n"
                            + RestaurantStatusHelper.getStatusSubtitle(restaurant))
                    .setPositiveButton("View Menu Anyway", (d, w) -> navigateToRestaurant(restaurant))
                    .setNegativeButton("Go Back", null)
                    .show();
            return;
        }
        navigateToRestaurant(restaurant);
    }

    private void navigateToRestaurant(Restaurant restaurant) {
        Intent intent = new Intent(this, RestaurantMenuActivity.class);
        intent.putExtra(Constants.EXTRA_RESTAURANT_ID,   restaurant.getRestaurantId());
        intent.putExtra(Constants.EXTRA_RESTAURANT_NAME, restaurant.getName());
        startActivity(intent);
    }

    private Restaurant getRestaurantById(String restaurantId) {
        if (restaurantId == null) return null;
        for (Restaurant r : allRestaurantsFull)
            if (restaurantId.equals(r.getRestaurantId())) return r;
        return null;
    }

    private String getRestaurantNameById(String restaurantId) {
        Restaurant r = getRestaurantById(restaurantId);
        return r != null ? r.getName() : "Restaurant";
    }

    // ── Firebase data loading ─────────────────────────────────────────────────

    private void loadCategories() {
        dbRef.child(Constants.NODE_CATEGORIES).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                categoryList.add(new Category("all", "All", null));
                int idx = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Category cat = snap.getValue(Category.class);
                    if (cat != null) {
                        cat.setCategoryId(snap.getKey());
                        if (cat.getCategoryImage() == null || cat.getCategoryImage().isEmpty())
                            cat.setCategoryImage("emoji:" + CATEGORY_EMOJIS[idx % CATEGORY_EMOJIS.length]);
                        categoryList.add(cat);
                        idx++;
                    }
                }
                categoryAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void loadRestaurants() {
        dbRef.child(Constants.NODE_RESTAURANTS).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                allRestaurantsFull.clear(); allRestaurants.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        Restaurant r = snap.getValue(Restaurant.class);
                        if (r != null) {
                            r.setRestaurantId(snap.getKey());
                            String img = snap.child("imageBase64").getValue(String.class);
                            if (img != null && !img.isEmpty()) r.setImageBase64(img);
                            Float avg = snap.child("avgRating").getValue(Float.class);
                            if (avg != null) r.setRating(avg);

                            // Read open/close fields
                            Boolean openVal   = snap.child("isOpen").getValue(Boolean.class);
                            Boolean manualVal = snap.child("manualOverride").getValue(Boolean.class);
                            String  openT     = snap.child("openTime").getValue(String.class);
                            String  closeT    = snap.child("closeTime").getValue(String.class);
                            if (openVal   != null) r.setOpen(openVal);
                            if (manualVal != null) r.setManualOverride(manualVal);
                            if (openT     != null && !openT.isEmpty())  r.setOpenTime(openT);
                            if (closeT    != null && !closeT.isEmpty()) r.setCloseTime(closeT);

                            allRestaurantsFull.add(r);
                            if (!isVegMode || r.isVegOnly()) allRestaurants.add(r);
                        }
                    } catch (Exception ignored) {}
                }
                applyAllFilters();
                updateCartBar();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void loadFoodItems() {
        binding.progressBar.setVisibility(View.VISIBLE);
        dbRef.child(Constants.NODE_FOOD_ITEMS).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                allFoodList.clear(); foodList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        FoodItem food = snap.getValue(FoodItem.class);
                        if (food == null) continue;
                        food.setFoodId(snap.getKey());
                        String rid = food.getRestaurantId();
                        if (rid == null || rid.isEmpty()) continue;
                        if (!food.isAvailable()) continue;
                        allFoodList.add(food);
                        if (!isVegMode || food.isVeg()) foodList.add(food);
                    } catch (Exception ignored) {}
                }
                foodAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    // ── Cart — single restaurant enforcement ─────────────────────────────────

    private void addToCartFromDashboard(FoodItem food) {
        String userId = sessionManager.getUid();
        if (userId == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show(); return;
        }
        String foodId  = food.getFoodId();
        String foodRid = food.getRestaurantId();
        if (foodId == null || foodId.isEmpty() || foodRid == null || foodRid.isEmpty()) {
            Toast.makeText(this, "Cannot add — restaurant info missing", Toast.LENGTH_SHORT).show(); return;
        }

        DatabaseReference cartRef = dbRef.child(Constants.NODE_USERS)
                .child(userId).child(Constants.NODE_CART);

        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    // Check if cart already has items from a different restaurant
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        String existingRid = snap.child("restaurantId").getValue(String.class);
                        if (existingRid != null && !existingRid.equals(foodRid)) {
                            String existingRName = getRestaurantNameById(existingRid);
                            String newRName      = getRestaurantNameById(foodRid);

                            // Show MODERN replace-cart dialog
                            showReplaceCartDialog(existingRName, newRName, food, userId, foodRid, cartRef);
                            return;
                        }
                    }
                }
                doWriteCartItem(food, userId, foodRid);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    /**
     * Shows a beautiful bottom-sheet style dialog when user tries to add items
     * from a different restaurant — matches the Swiggy/Zomato experience.
     */
    private void showReplaceCartDialog(String oldRestaurant, String newRestaurant,
                                       FoodItem food, String userId, String foodRid,
                                       DatabaseReference cartRef) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_replace_cart, null);

        android.widget.TextView tvOldRest = dialogView.findViewById(R.id.tvOldRestaurant);
        android.widget.TextView tvNewRest = dialogView.findViewById(R.id.tvNewRestaurant);
        tvOldRest.setText(oldRestaurant);
        tvNewRest.setText(newRestaurant);

        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                .setView(dialogView)
                .setPositiveButton("Start Fresh 🗑", (d, w) ->
                        cartRef.removeValue().addOnSuccessListener(u ->
                                doWriteCartItem(food, userId, foodRid)))
                .setNegativeButton("Keep Cart", null)
                .show();
    }

    private void doWriteCartItem(FoodItem food, String userId, String foodRid) {
        DatabaseReference itemRef = dbRef.child(Constants.NODE_USERS).child(userId)
                .child(Constants.NODE_CART).child(food.getFoodId());
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long qty = snapshot.child("quantity").getValue(Long.class);
                    int newQty = (qty != null ? qty.intValue() : 1) + 1;
                    Map<String, Object> u = new HashMap<>();
                    u.put("quantity",   newQty);
                    u.put("totalPrice", food.getPrice() * newQty);
                    itemRef.updateChildren(u);
                    Toast.makeText(UserDashboardActivity.this,
                            food.getName() + " qty updated 🛒", Toast.LENGTH_SHORT).show();
                } else {
                    Map<String, Object> cartData = new HashMap<>();
                    cartData.put("foodId",       food.getFoodId());
                    cartData.put("name",         food.getName());
                    cartData.put("price",        food.getPrice());
                    cartData.put("quantity",     1);
                    cartData.put("totalPrice",   food.getPrice());
                    cartData.put("restaurantId", foodRid);
                    itemRef.setValue(cartData).addOnSuccessListener(x -> {
                        Toast.makeText(UserDashboardActivity.this,
                                food.getName() + " added to cart 🛒", Toast.LENGTH_SHORT).show();
                        updateCartBar();
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }
}