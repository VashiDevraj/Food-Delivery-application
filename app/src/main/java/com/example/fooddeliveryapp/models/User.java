package com.example.fooddeliveryapp.models;

/**
 * User — stored at:  Users/{userId}/profile
 *
 * The node key is the Firebase UID.
 * Cart and Orders live as siblings of profile:
 *
 *   Users/
 *     {userId}/
 *       profile/   ← this object
 *       Cart/
 *       Orders/
 */
public class User {

    private String userId;
    private String name;
    private String email;
    private String role;       // "user"
    private String phone;
    private String city;
    private String pincode;
    private String address;    // full address e.g. "12, MG Road, Surat - 395001"

    // Required empty constructor for Firebase deserialization
    public User() {}

    /** Minimal constructor used at registration */
    public User(String userId, String name, String email, String role) {
        this.userId = userId;
        this.name   = name;
        this.email  = email;
        this.role   = role;
        this.phone   = "";
        this.city    = "";
        this.pincode = "";
        this.address = "";
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getUserId()  { return userId; }
    public String getName()    { return name   != null ? name   : ""; }
    public String getEmail()   { return email  != null ? email  : ""; }
    public String getRole()    { return role   != null ? role   : ""; }
    public String getPhone()   { return phone  != null ? phone  : ""; }
    public String getCity()    { return city   != null ? city   : ""; }
    public String getPincode() { return pincode!= null ? pincode: ""; }
    public String getAddress() { return address!= null ? address: ""; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setUserId(String userId)   { this.userId  = userId; }
    public void setName(String name)       { this.name    = name; }
    public void setEmail(String email)     { this.email   = email; }
    public void setRole(String role)       { this.role    = role; }
    public void setPhone(String phone)     { this.phone   = phone; }
    public void setCity(String city)       { this.city    = city; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public void setAddress(String address) { this.address = address; }
}