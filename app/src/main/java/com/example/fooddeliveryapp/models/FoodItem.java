package com.example.fooddeliveryapp.models;

/**
 * FoodItem model.
 *
 * Firebase fields: foodId, restaurantId, categoryId, name, description,
 *                  price, imageBase64, veg, available
 *
 * available = true by default (admin can toggle off to hide from users)
 */
public class FoodItem {

    private String  foodId;
    private String  restaurantId;
    private String  categoryId;
    private String  name;
    private String  description;
    private double  price;
    private String  imageBase64;
    private boolean veg;
    private boolean available = true;   // NEW: admin can disable items
    private String offer;
    /** Required no-arg constructor for Firebase */
    public FoodItem() {}

    /** Full constructor used when creating/saving items */
    public FoodItem(String foodId, String restaurantId, String categoryId,
                    String name, String description, double price,
                    String imageBase64, boolean veg) {
        this.foodId       = foodId;
        this.restaurantId = restaurantId;
        this.categoryId   = categoryId;
        this.name         = name;
        this.description  = description;
        this.price        = price;
        this.imageBase64  = imageBase64;
        this.veg          = veg;
        this.available    = true;
        this.offer        = offer;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String  getFoodId()       { return foodId;       }
    public String  getRestaurantId() { return restaurantId; }
    public String  getCategoryId()   { return categoryId;   }
    public String  getName()         { return name;         }
    public String  getDescription()  { return description;  }
    public double  getPrice()        { return price;        }
    public String  getImageBase64()  { return imageBase64;  }
    public boolean isVeg()           { return veg;          }
    public boolean isAvailable()     { return available;    }
    public String getOffer() {
        return offer;
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setFoodId(String foodId)             { this.foodId       = foodId;       }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }
    public void setCategoryId(String categoryId)     { this.categoryId   = categoryId;   }
    public void setName(String name)                 { this.name         = name;         }
    public void setDescription(String desc)          { this.description  = desc;         }
    public void setPrice(double price)               { this.price        = price;        }
    public void setImageBase64(String img)           { this.imageBase64  = img;          }
    public void setVeg(boolean veg)                  { this.veg          = veg;          }
    public void setAvailable(boolean available)      { this.available    = available;    }
    public void setOffer(String offer) {
        this.offer = offer;
    }
}