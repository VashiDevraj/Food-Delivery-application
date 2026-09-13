package com.example.fooddeliveryapp.utils;

import com.example.fooddeliveryapp.models.Restaurant;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * RestaurantStatusHelper
 *
 * Centralises all open/close logic so the same rules apply on both
 * the Admin and User sides.
 *
 * Firebase schema under Restaurants/{restaurantId}:
 *   isOpen          : boolean
 *   manualOverride  : boolean
 *   openTime        : "HH:mm"  (24-hour)
 *   closeTime       : "HH:mm"  (24-hour)
 */
public class RestaurantStatusHelper {

    // ── Status resolution ─────────────────────────────────────────────────────

    /**
     * Returns true if the restaurant is effectively open RIGHT NOW.
     *
     * Rules:
     *  1. manualOverride == true  → return isOpen (admin override wins)
     *  2. manualOverride == false → compute from schedule
     */
    public static boolean isOpen(boolean isOpenFlag, boolean manualOverride,
                                 String openTime, String closeTime) {
        if (manualOverride) return isOpenFlag;
        return isInSchedule(openTime, closeTime);
    }

    /** Convenience overload accepting a Restaurant model. */
    public static boolean isOpen(Restaurant r) {
        return isOpen(r.isOpen(), r.isManualOverride(), r.getOpenTime(), r.getCloseTime());
    }

    /**
     * Returns true if the current local time falls within [openTime, closeTime).
     * Handles overnight ranges (e.g. "22:00" → "03:00").
     */
    public static boolean isInSchedule(String openTime, String closeTime) {
        try {
            Calendar cal   = Calendar.getInstance();
            int nowMins    = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
            int openMins   = parseTimeMins(openTime);
            int closeMins  = parseTimeMins(closeTime);

            if (openMins <= closeMins) {
                return nowMins >= openMins && nowMins < closeMins;
            } else {
                return nowMins >= openMins || nowMins < closeMins;
            }
        } catch (Exception e) {
            return true;
        }
    }

    /** Parses "HH:mm" → minutes since midnight. */
    public static int parseTimeMins(String time) {
        if (time == null || !time.contains(":")) return 0;
        String[] p = time.split(":");
        return Integer.parseInt(p[0].trim()) * 60 + Integer.parseInt(p[1].trim());
    }

    // ── Firebase writes ───────────────────────────────────────────────────────

    /**
     * Persists a MANUAL toggle to Firebase.
     * Call this when the admin flips the switch explicitly.
     *
     * @param restaurantId    Firebase key under /Restaurants
     * @param open            The desired open/closed state
     */
    public static void setManualStatus(String restaurantId, boolean open) {
        DatabaseReference ref = getRestaurantRef(restaurantId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("isOpen",         open);
        updates.put("manualOverride", true);
        ref.updateChildren(updates);

        // Also write to the admin's own Restaurant/info node for consistency
        // (caller should do this if they have adminId)
    }

    /**
     * Persists schedule (openTime, closeTime) to Firebase and switches to AUTO mode.
     *
     * @param restaurantId  Firebase key under /Restaurants
     * @param openTime      "HH:mm"
     * @param closeTime     "HH:mm"
     */
    public static void setSchedule(String restaurantId,
                                   String openTime, String closeTime) {
        DatabaseReference ref = getRestaurantRef(restaurantId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("openTime",       openTime);
        updates.put("closeTime",      closeTime);
        updates.put("manualOverride", false);
        // Immediately compute and persist the current effective state
        updates.put("isOpen",         isInSchedule(openTime, closeTime));
        ref.updateChildren(updates);
    }

    /**
     * Clears a manual override so the restaurant reverts to schedule-based status.
     */
    public static void clearManualOverride(String restaurantId,
                                           String openTime, String closeTime) {
        DatabaseReference ref = getRestaurantRef(restaurantId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("manualOverride", false);
        updates.put("isOpen",         isInSchedule(openTime, closeTime));
        ref.updateChildren(updates);
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    /** "Opens at 9:00 AM"  or  "Closes at 10:00 PM" */
    public static String getStatusSubtitle(Restaurant r) {
        boolean open = isOpen(r);
        if (open) {
            return "Closes at " + formatTime(r.getCloseTime());
        } else {
            return "Opens at " + formatTime(r.getOpenTime());
        }
    }

    /** Converts "HH:mm" → "h:mm AM/PM" */
    public static String formatTime(String hhmm) {
        try {
            String[] p  = hhmm.split(":");
            int h = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            String suf  = h >= 12 ? "PM" : "AM";
            int h12     = h % 12;
            if (h12 == 0) h12 = 12;
            return String.format("%d:%02d %s", h12, m, suf);
        } catch (Exception e) {
            return hhmm;
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private static DatabaseReference getRestaurantRef(String restaurantId) {
        return FirebaseDatabase
                .getInstance(Constants.FIREBASE_URL)
                .getReference()
                .child(Constants.NODE_RESTAURANTS)
                .child(restaurantId);
    }
}