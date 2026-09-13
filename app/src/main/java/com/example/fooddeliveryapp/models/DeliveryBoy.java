package com.example.fooddeliveryapp.models;

/**
 * DeliveryBoy model — stored at DeliveryBoys/{uid}/profile
 */
public class DeliveryBoy {

    private String  uid;
    private String  name;
    private String  email;
    private String  phone;
    private String  vehicleType;      // "Bike" | "Scooter" | "Bicycle" | "Car"
    private String  vehicleNumber;
    private boolean isOnline;
    private float   avgRating;
    private int     totalRatings;
    private int     totalDeliveries;
    private double  totalEarnings;
    private double  todayEarnings;
    private double  monthEarnings;
    private long    registeredAt;
    private String  fcmToken;         // for push notifications
    private String  profileImageUrl;
    private String  role;             // always "delivery_boy"

    public DeliveryBoy() {}

    public DeliveryBoy(String uid, String name, String email, String phone,
                       String vehicleType, String vehicleNumber) {
        this.uid           = uid;
        this.name          = name;
        this.email         = email;
        this.phone         = phone;
        this.vehicleType   = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.isOnline      = false;
        this.avgRating     = 0f;
        this.totalRatings  = 0;
        this.totalDeliveries = 0;
        this.totalEarnings = 0;
        this.todayEarnings = 0;
        this.monthEarnings = 0;
        this.registeredAt  = System.currentTimeMillis();
        this.role          = "delivery_boy";
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getUid()              { return uid != null ? uid : ""; }
    public String  getName()             { return name != null ? name : ""; }
    public String  getEmail()            { return email != null ? email : ""; }
    public String  getPhone()            { return phone != null ? phone : ""; }
    public String  getVehicleType()      { return vehicleType != null ? vehicleType : "Bike"; }
    public String  getVehicleNumber()    { return vehicleNumber != null ? vehicleNumber : ""; }
    public boolean isOnline()            { return isOnline; }
    public float   getAvgRating()        { return avgRating; }
    public int     getTotalRatings()     { return totalRatings; }
    public int     getTotalDeliveries()  { return totalDeliveries; }
    public double  getTotalEarnings()    { return totalEarnings; }
    public double  getTodayEarnings()    { return todayEarnings; }
    public double  getMonthEarnings()    { return monthEarnings; }
    public long    getRegisteredAt()     { return registeredAt; }
    public String  getFcmToken()         { return fcmToken != null ? fcmToken : ""; }
    public String  getProfileImageUrl()  { return profileImageUrl != null ? profileImageUrl : ""; }
    public String  getRole()             { return role != null ? role : "delivery_boy"; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setUid(String uid)                       { this.uid = uid; }
    public void setName(String name)                     { this.name = name; }
    public void setEmail(String email)                   { this.email = email; }
    public void setPhone(String phone)                   { this.phone = phone; }
    public void setVehicleType(String vehicleType)       { this.vehicleType = vehicleType; }
    public void setVehicleNumber(String vehicleNumber)   { this.vehicleNumber = vehicleNumber; }
    public void setOnline(boolean online)                { isOnline = online; }
    public void setAvgRating(float avgRating)            { this.avgRating = avgRating; }
    public void setTotalRatings(int totalRatings)        { this.totalRatings = totalRatings; }
    public void setTotalDeliveries(int totalDeliveries)  { this.totalDeliveries = totalDeliveries; }
    public void setTotalEarnings(double totalEarnings)   { this.totalEarnings = totalEarnings; }
    public void setTodayEarnings(double todayEarnings)   { this.todayEarnings = todayEarnings; }
    public void setMonthEarnings(double monthEarnings)   { this.monthEarnings = monthEarnings; }
    public void setRegisteredAt(long registeredAt)       { this.registeredAt = registeredAt; }
    public void setFcmToken(String fcmToken)             { this.fcmToken = fcmToken; }
    public void setProfileImageUrl(String url)           { this.profileImageUrl = url; }
    public void setRole(String role)                     { this.role = role; }

    // ── Utility ───────────────────────────────────────────────────────────────
    public String getRatingDisplay() {
        if (totalRatings == 0) return "No ratings yet";
        return String.format("%.1f ⭐ (%d ratings)", avgRating, totalRatings);
    }

    public String getVehicleIcon() {
        if (vehicleType == null) return "🛵";
        switch (vehicleType) {
            case "Bike":    return "🏍️";
            case "Bicycle": return "🚲";
            case "Car":     return "🚗";
            default:        return "🛵";
        }
    }
}