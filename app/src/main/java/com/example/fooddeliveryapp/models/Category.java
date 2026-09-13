package com.example.fooddeliveryapp.models;

public class Category {

    private String categoryId;
    private String categoryName;
    private String categoryImage; // Base64 image

    public Category(){}

    public Category(String id, String name, String image){
        this.categoryId = id;
        this.categoryName = name;
        this.categoryImage = image;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryImage() {
        return categoryImage;
    }

    public void setCategoryImage(String categoryImage) {
        this.categoryImage = categoryImage;
    }
}