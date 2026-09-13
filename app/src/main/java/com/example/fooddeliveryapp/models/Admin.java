// ══════════════════════════════════════════════════════════════════════════════
// FILE 1: Admin.java
// Stored at: Admins/{adminId}/profile
// ══════════════════════════════════════════════════════════════════════════════
package com.example.fooddeliveryapp.models;

public class Admin {

    private String adminId;
    private String name;
    private String email;
    private String role;  // always "admin"

    public Admin() {}

    public Admin(String adminId, String name, String email) {
        this.adminId = adminId;
        this.name    = name;
        this.email   = email;
        this.role    = "admin";
    }

    public String getAdminId() { return adminId; }
    public String getName()    { return name  != null ? name  : ""; }
    public String getEmail()   { return email != null ? email : ""; }
    public String getRole()    { return role  != null ? role  : "admin"; }

    public void setAdminId(String adminId) { this.adminId = adminId; }
    public void setName(String name)       { this.name    = name; }
    public void setEmail(String email)     { this.email   = email; }
    public void setRole(String role)       { this.role    = role; }
}