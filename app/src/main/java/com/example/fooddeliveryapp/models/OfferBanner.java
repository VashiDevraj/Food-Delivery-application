package com.example.fooddeliveryapp.models;

/**
 * OfferBanner — stored at: banners/{bannerId}
 *
 * Represents a promotional offer banner shown on the user dashboard.
 * Can be backed by either a remote URL or a base64-encoded image.
 */
public class OfferBanner {

    private String bannerId;
    private String title;           // e.g. "50% OFF"
    private String subtitle;        // e.g. "on your first order"
    private String badgeText;       // e.g. "LIMITED TIME"
    private String imageUrl;        // remote URL (optional)
    private String imageBase64;     // base64 fallback (optional)
    private String bgColorStart;    // gradient start hex e.g. "#E23744"
    private String bgColorEnd;      // gradient end hex   e.g. "#C62828"
    private String ctaText;         // call-to-action label e.g. "Order Now"
    private String linkedRestaurantId; // optional — taps open this restaurant
    private int    sortOrder;       // display order (ascending)
    private boolean active;         // admin can deactivate without deleting

    // Firebase empty constructor
    public OfferBanner() {}

    public OfferBanner(String bannerId, String title, String subtitle,
                       String badgeText, String bgColorStart, String bgColorEnd,
                       String ctaText) {
        this.bannerId      = bannerId;
        this.title         = title;
        this.subtitle      = subtitle;
        this.badgeText     = badgeText;
        this.bgColorStart  = bgColorStart;
        this.bgColorEnd    = bgColorEnd;
        this.ctaText       = ctaText;
        this.active        = true;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getBannerId()           { return bannerId           != null ? bannerId           : ""; }
    public String  getTitle()              { return title              != null ? title              : ""; }
    public String  getSubtitle()           { return subtitle           != null ? subtitle           : ""; }
    public String  getBadgeText()          { return badgeText          != null ? badgeText          : ""; }
    public String  getImageUrl()           { return imageUrl           != null ? imageUrl           : ""; }
    public String  getImageBase64()        { return imageBase64        != null ? imageBase64        : ""; }
    public String  getBgColorStart()       { return bgColorStart       != null ? bgColorStart       : "#E23744"; }
    public String  getBgColorEnd()         { return bgColorEnd         != null ? bgColorEnd         : "#C62828"; }
    public String  getCtaText()            { return ctaText            != null ? ctaText            : "Order Now"; }
    public String  getLinkedRestaurantId() { return linkedRestaurantId != null ? linkedRestaurantId : ""; }
    public int     getSortOrder()          { return sortOrder; }
    public boolean isActive()              { return active; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setBannerId(String v)           { this.bannerId           = v; }
    public void setTitle(String v)              { this.title              = v; }
    public void setSubtitle(String v)           { this.subtitle           = v; }
    public void setBadgeText(String v)          { this.badgeText          = v; }
    public void setImageUrl(String v)           { this.imageUrl           = v; }
    public void setImageBase64(String v)        { this.imageBase64        = v; }
    public void setBgColorStart(String v)       { this.bgColorStart       = v; }
    public void setBgColorEnd(String v)         { this.bgColorEnd         = v; }
    public void setCtaText(String v)            { this.ctaText            = v; }
    public void setLinkedRestaurantId(String v) { this.linkedRestaurantId = v; }
    public void setSortOrder(int v)             { this.sortOrder          = v; }
    public void setActive(boolean v)            { this.active             = v; }
}