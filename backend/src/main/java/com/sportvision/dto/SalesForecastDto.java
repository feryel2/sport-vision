package com.sportvision.dto;

public class SalesForecastDto {
    private Long productId;
    private String productName;
    private String categoryName;
    private int currentStock;
    private int predictedSalesNext30Days;
    private int recommendedReorderQuantity;
    private String stockHealthStatus; // OPTIMAL, RISQUE_RUPTURE, CRITIQUE, SURSTOCK
    private double confidenceScore;
    private String trend; // HAUSSE, STABLE, BAISSE
    private String reason;

    public SalesForecastDto() {}

    public SalesForecastDto(Long productId, String productName, String categoryName, int currentStock,
                            int predictedSalesNext30Days, int recommendedReorderQuantity,
                            String stockHealthStatus, double confidenceScore, String trend, String reason) {
        this.productId = productId;
        this.productName = productName;
        this.categoryName = categoryName;
        this.currentStock = currentStock;
        this.predictedSalesNext30Days = predictedSalesNext30Days;
        this.recommendedReorderQuantity = recommendedReorderQuantity;
        this.stockHealthStatus = stockHealthStatus;
        this.confidenceScore = confidenceScore;
        this.trend = trend;
        this.reason = reason;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getPredictedSalesNext30Days() {
        return predictedSalesNext30Days;
    }

    public void setPredictedSalesNext30Days(int predictedSalesNext30Days) {
        this.predictedSalesNext30Days = predictedSalesNext30Days;
    }

    public int getRecommendedReorderQuantity() {
        return recommendedReorderQuantity;
    }

    public void setRecommendedReorderQuantity(int recommendedReorderQuantity) {
        this.recommendedReorderQuantity = recommendedReorderQuantity;
    }

    public String getStockHealthStatus() {
        return stockHealthStatus;
    }

    public void setStockHealthStatus(String stockHealthStatus) {
        this.stockHealthStatus = stockHealthStatus;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
