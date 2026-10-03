package com.sportvision.dto;

import com.sportvision.entity.Product;

public class ProductRecommendationDto {
    private Product product;
    private int matchPercentage;
    private String recommendationReason;
    private String badge;

    public ProductRecommendationDto() {}

    public ProductRecommendationDto(Product product, int matchPercentage, String recommendationReason, String badge) {
        this.product = product;
        this.matchPercentage = matchPercentage;
        this.recommendationReason = recommendationReason;
        this.badge = badge;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(int matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }
}
