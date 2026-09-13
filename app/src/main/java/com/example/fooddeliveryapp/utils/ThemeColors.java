package com.example.fooddeliveryapp.utils;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.example.fooddeliveryapp.R;

public class ThemeColors {

    public static int background(Context c) {
        return ContextCompat.getColor(c, R.color.lightBackground);
    }

    public static int surface(Context c) {
        return ContextCompat.getColor(c, R.color.lightSurface);
    }

    public static int onSurface(Context c) {
        return ContextCompat.getColor(c, R.color.lightOnSurface);
    }

    public static int onSurfaceVariant(Context c) {
        return ContextCompat.getColor(c, R.color.lightOnSurfaceVariant);
    }

    public static int divider(Context c) {
        return ContextCompat.getColor(c, R.color.lightDivider);
    }

    public static int primary(Context c) {
        return ContextCompat.getColor(c, R.color.primaryColor);
    }
}