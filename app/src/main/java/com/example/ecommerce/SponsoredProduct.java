package com.example.ecommerce;
public class SponsoredProduct {
    private final String name;
    private final int imageResId;

    public SponsoredProduct(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
}
