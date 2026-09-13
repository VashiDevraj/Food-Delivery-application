package com.example.fooddeliveryapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {

    private static final String PREF_SESSION  = "FoodAppSession";
    private static final String PREF_SETTINGS = "AppSettings";

    private static final String KEY_LOGGED_IN            = "is_logged_in";
    private static final String KEY_UID                  = "uid";
    private static final String KEY_NAME                 = "name";
    private static final String KEY_EMAIL                = "email";
    private static final String KEY_ROLE                 = "role";
    private static final String KEY_RESTAURANT_DONE      = "restaurant_setup_done";
    private static final String KEY_PHONE                = "user_phone";
    private static final String KEY_SAVED_ADDRESSES      = "saved_addresses";
    private static final String KEY_SELECTED_ADDR_IDX    = "selected_address_idx";
    private static final String KEY_VEG_MODE             = "veg_mode";
    private static final String KEY_PERSONALISED_RATINGS = "personalised_ratings";
    // Delivery Boy keys
    private static final String KEY_VEHICLE_NUMBER       = "vehicle_number";
    private static final String KEY_VEHICLE_TYPE         = "vehicle_type";
    private static final String KEY_IS_ONLINE            = "is_online";

    private static final String KEY_ADDRESS = "user_address";
    private static final String KEY_CITY    = "user_city";
    private static final String KEY_PINCODE = "user_pincode";

    private final SharedPreferences        session;
    private final SharedPreferences        settings;
    private final SharedPreferences.Editor sessionEditor;

    public SessionManager(Context context) {
        Context app   = context.getApplicationContext();
        session       = app.getSharedPreferences(PREF_SESSION,  Context.MODE_PRIVATE);
        settings      = app.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE);
        sessionEditor = session.edit();
    }

    public SharedPreferences getSharedPreferences() { return session; }

    // =========================================================================
    //  ADDRESS MODEL
    // =========================================================================
    public static class Address {
        public String label;
        public String houseNo;
        public String street;
        public String landmark;
        public String city;
        public String pincode;
        public String fullAddress;

        public Address() {}

        public Address(String label, String houseNo, String street,
                       String landmark, String city, String pincode) {
            this.label    = label;
            this.houseNo  = houseNo;
            this.street   = street;
            this.landmark = landmark;
            this.city     = city;
            this.pincode  = pincode;
            this.fullAddress = buildFull(houseNo, street, landmark, city, pincode);
        }

        private static String buildFull(String h, String s, String l, String c, String p) {
            StringBuilder sb = new StringBuilder();
            if (!emp(h)) sb.append(h);
            if (!emp(s)) add(sb, s);
            if (!emp(l)) add(sb, l);
            if (!emp(c)) add(sb, c);
            if (!emp(p)) sb.append(" - ").append(p);
            return sb.toString();
        }

        private static void add(StringBuilder sb, String v) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(v);
        }

        private static boolean emp(String s) { return s == null || s.trim().isEmpty(); }

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("label", safe(label));
            o.put("houseNo", safe(houseNo));
            o.put("street", safe(street));
            o.put("landmark", safe(landmark));
            o.put("city", safe(city));
            o.put("pincode", safe(pincode));
            o.put("fullAddress", safe(fullAddress));
            return o;
        }

        public static Address fromJson(JSONObject o) {
            Address a = new Address();
            a.label       = o.optString("label",       "Home");
            a.houseNo     = o.optString("houseNo",     "");
            a.street      = o.optString("street",      "");
            a.landmark    = o.optString("landmark",    "");
            a.city        = o.optString("city",        "");
            a.pincode     = o.optString("pincode",     "");
            a.fullAddress = o.optString("fullAddress", "");
            return a;
        }

        private static String safe(String s) { return s != null ? s : ""; }

        public String getDisplayLabel() {
            if (!emp(label)) return label;
            if (!emp(city))  return city;
            return "Address";
        }
    }

    // =========================================================================
    //  SESSION SAVE
    // =========================================================================
    public void saveSession(String uid, String name, String role) {
        sessionEditor
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_UID,  safeStr(uid))
                .putString(KEY_NAME, safeStr(name))
                .putString(KEY_ROLE, safeStr(role))
                .apply();
    }

    public void saveSession(String uid, String name, String role, String email) {
        sessionEditor
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_UID,   safeStr(uid))
                .putString(KEY_NAME,  safeStr(name))
                .putString(KEY_ROLE,  safeStr(role))
                .putString(KEY_EMAIL, safeStr(email))
                .apply();
    }

    /** Save delivery boy specific session data */
    public void saveDeliveryBoySession(String uid, String name, String email,
                                       String phone, String vehicleType, String vehicleNumber) {
        sessionEditor
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_UID,            safeStr(uid))
                .putString(KEY_NAME,           safeStr(name))
                .putString(KEY_EMAIL,          safeStr(email))
                .putString(KEY_ROLE,           Constants.ROLE_DELIVERY_BOY)
                .putString(KEY_PHONE,          safeStr(phone))
                .putString(KEY_VEHICLE_TYPE,   safeStr(vehicleType))
                .putString(KEY_VEHICLE_NUMBER, safeStr(vehicleNumber))
                .apply();
    }

    // =========================================================================
    //  PROFILE FIELDS
    // =========================================================================
    public void saveUserName(String name)   { sessionEditor.putString(KEY_NAME,  safeStr(name)).apply(); }
    public void saveUserEmail(String email) { sessionEditor.putString(KEY_EMAIL, safeStr(email)).apply(); }
    public void saveUserPhone(String phone) { sessionEditor.putString(KEY_PHONE, safeStr(phone)).apply(); }

    public String getUid()           { return session.getString(KEY_UID,            ""); }
    public String getName()          { return session.getString(KEY_NAME,           ""); }
    public String getEmail()         { return session.getString(KEY_EMAIL,          ""); }
    public String getRole()          { return session.getString(KEY_ROLE,           Constants.ROLE_USER); }
    public String getUserPhone()     { return session.getString(KEY_PHONE,          ""); }
    public String getVehicleType()   { return session.getString(KEY_VEHICLE_TYPE,   ""); }
    public String getVehicleNumber() { return session.getString(KEY_VEHICLE_NUMBER, ""); }

    public boolean isLoggedIn()    { return session.getBoolean(KEY_LOGGED_IN, false); }
    public boolean isAdmin()       { return Constants.ROLE_ADMIN.equals(getRole()); }
    public boolean isDeliveryBoy() { return Constants.ROLE_DELIVERY_BOY.equals(getRole()); }

    public void setOnlineStatus(boolean online) {
        sessionEditor.putBoolean(KEY_IS_ONLINE, online).apply();
    }

    public boolean isOnline() { return session.getBoolean(KEY_IS_ONLINE, false); }

    // =========================================================================
    //  RESTAURANT SETUP
    // =========================================================================
    public void setRestaurantSetupDone() { sessionEditor.putBoolean(KEY_RESTAURANT_DONE, true).apply(); }
    public boolean isRestaurantSetupDone() { return session.getBoolean(KEY_RESTAURANT_DONE, false); }

    // =========================================================================
    //  PREFERENCES
    // =========================================================================
    public void setVegMode(boolean enabled)          { sessionEditor.putBoolean(KEY_VEG_MODE,             enabled).apply(); }
    public boolean isVegMode()                       { return session.getBoolean(KEY_VEG_MODE,             false); }
    public void setPersonalisedRatings(boolean on)   { sessionEditor.putBoolean(KEY_PERSONALISED_RATINGS,  on).apply(); }
    public boolean isPersonalisedRatings()           { return session.getBoolean(KEY_PERSONALISED_RATINGS, false); }

    // =========================================================================
    //  APP SETTINGS
    // =========================================================================
    public void saveNightMode(int mode)  { settings.edit().putInt("night_mode",       mode).apply(); }
    public int  getNightMode()           { return settings.getInt("night_mode",        -1); }
    public void saveNotifOrders(boolean on){ settings.edit().putBoolean("notif_orders", on).apply(); }
    public boolean isNotifOrders()       { return settings.getBoolean("notif_orders",  true); }
    public void saveNotifPromo(boolean on){ settings.edit().putBoolean("notif_promo",   on).apply(); }
    public boolean isNotifPromo()        { return settings.getBoolean("notif_promo",   true); }
    public void saveTextSize(int size)   { settings.edit().putInt("text_size",         size).apply(); }
    public int  getTextSize()            { return settings.getInt("text_size",          1); }

    // =========================================================================
    //  MULTI-ADDRESS MANAGEMENT
    // =========================================================================
    public List<Address> getSavedAddresses() {
        List<Address> list = new ArrayList<>();
        String json = session.getString(KEY_SAVED_ADDRESSES, "");
        if (json == null || json.isEmpty()) {
            String legacy = session.getString(KEY_ADDRESS, "");
            if (legacy != null && !legacy.isEmpty()) {
                Address a = new Address();
                a.label = "Home"; a.city = session.getString(KEY_CITY, "");
                a.pincode = session.getString(KEY_PINCODE, ""); a.fullAddress = legacy;
                list.add(a);
                persistAddressList(list);
                sessionEditor.putInt(KEY_SELECTED_ADDR_IDX, 0).apply();
            }
            return list;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(Address.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {}
        return list;
    }

    public void addAddress(Address address) {
        List<Address> list = getSavedAddresses();
        list.add(address);
        persistAddressList(list);
        int newIdx = list.size() - 1;
        sessionEditor.putInt(KEY_SELECTED_ADDR_IDX, newIdx)
                .putString(KEY_ADDRESS, address.fullAddress)
                .putString(KEY_CITY,    address.city)
                .putString(KEY_PINCODE, address.pincode)
                .apply();
    }

    public void updateAddress(int index, Address address) {
        List<Address> list = getSavedAddresses();
        if (index < 0 || index >= list.size()) return;
        list.set(index, address);
        persistAddressList(list);
        if (index == getSelectedAddressIndex()) syncLegacyKeys(address);
    }

    public void deleteAddress(int index) {
        List<Address> list = getSavedAddresses();
        if (index < 0 || index >= list.size()) return;
        list.remove(index);
        persistAddressList(list);
        int selected = getSelectedAddressIndex();
        if (list.isEmpty()) {
            sessionEditor.putInt(KEY_SELECTED_ADDR_IDX, -1)
                    .putString(KEY_ADDRESS, "").putString(KEY_CITY, "").putString(KEY_PINCODE, "").apply();
        } else if (selected > index) {
            sessionEditor.putInt(KEY_SELECTED_ADDR_IDX, selected - 1).apply();
        } else if (selected == index) {
            int newIdx = Math.min(index, list.size() - 1);
            sessionEditor.putInt(KEY_SELECTED_ADDR_IDX, newIdx).apply();
            syncLegacyKeys(list.get(newIdx));
        }
    }

    public void selectAddress(int index) {
        List<Address> list = getSavedAddresses();
        if (index < 0 || index >= list.size()) return;
        sessionEditor.putInt(KEY_SELECTED_ADDR_IDX, index).apply();
        syncLegacyKeys(list.get(index));
    }

    public int     getSelectedAddressIndex() { return session.getInt(KEY_SELECTED_ADDR_IDX, -1); }

    public Address getSelectedAddress() {
        List<Address> list = getSavedAddresses();
        if (list.isEmpty()) return null;
        int idx = getSelectedAddressIndex();
        if (idx >= 0 && idx < list.size()) return list.get(idx);
        return list.get(list.size() - 1);
    }

    public String getUserAddress() {
        Address a = getSelectedAddress();
        return a != null ? a.fullAddress : session.getString(KEY_ADDRESS, "");
    }

    public String getUserCity() {
        Address a = getSelectedAddress();
        return a != null ? a.city : session.getString(KEY_CITY, "");
    }

    public String getUserPincode() {
        Address a = getSelectedAddress();
        return a != null ? a.pincode : session.getString(KEY_PINCODE, "");
    }

    public void saveUserAddress(String city, String fullAddress, String pincode) {
        List<Address> list = getSavedAddresses();
        int idx = getSelectedAddressIndex();
        Address a = new Address();
        a.label = "Home"; a.city = city; a.pincode = pincode; a.fullAddress = fullAddress;
        if (idx >= 0 && idx < list.size()) { list.set(idx, a); }
        else { list.add(a); idx = list.size() - 1; sessionEditor.putInt(KEY_SELECTED_ADDR_IDX, idx); }
        persistAddressList(list);
        syncLegacyKeys(a);
        sessionEditor.apply();
    }

    // =========================================================================
    //  LOGOUT
    // =========================================================================
    public void logout() {
        boolean restaurantDone = isRestaurantSetupDone();
        sessionEditor.clear();
        if (restaurantDone) sessionEditor.putBoolean(KEY_RESTAURANT_DONE, true);
        sessionEditor.apply();
    }

    public void clearNonAuthData() {
        String uid = getUid(); String role = getRole();
        String email = getEmail(); String name = getName();
        sessionEditor.clear()
                .putBoolean(KEY_LOGGED_IN, true).putString(KEY_UID, uid)
                .putString(KEY_ROLE, role).putString(KEY_EMAIL, email).putString(KEY_NAME, name).apply();
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================
    private void persistAddressList(List<Address> list) {
        JSONArray arr = new JSONArray();
        for (Address a : list) { try { arr.put(a.toJson()); } catch (JSONException ignored) {} }
        sessionEditor.putString(KEY_SAVED_ADDRESSES, arr.toString()).apply();
    }

    private void syncLegacyKeys(Address a) {
        sessionEditor.putString(KEY_ADDRESS, a.fullAddress)
                .putString(KEY_CITY, a.city).putString(KEY_PINCODE, a.pincode).apply();
    }

    private static String safeStr(String s) { return s != null ? s.trim() : ""; }
}