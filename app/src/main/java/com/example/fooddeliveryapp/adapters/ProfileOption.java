package com.example.fooddeliveryapp.adapters;

public class ProfileOption {
    private int iconResId;
    private String title;
    private String subtitle;

    public ProfileOption(int iconResId, String title, String subtitle) {
        this.iconResId = iconResId;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
}