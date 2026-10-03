package com.sportvision.dto;

import java.math.BigDecimal;
import java.util.Map;

public class DashboardStatsDto {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalProducts;
    private long totalCustomers;
    private long lowStockCount;
    private long pendingOrdersCount;
    private Map<String, Long> productsByCategory;
    private Map<String, BigDecimal> monthlySales;

    public DashboardStatsDto() {}

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public long getPendingOrdersCount() {
        return pendingOrdersCount;
    }

    public void setPendingOrdersCount(long pendingOrdersCount) {
        this.pendingOrdersCount = pendingOrdersCount;
    }

    public Map<String, Long> getProductsByCategory() {
        return productsByCategory;
    }

    public void setProductsByCategory(Map<String, Long> productsByCategory) {
        this.productsByCategory = productsByCategory;
    }

    public Map<String, BigDecimal> getMonthlySales() {
        return monthlySales;
    }

    public void setMonthlySales(Map<String, BigDecimal> monthlySales) {
        this.monthlySales = monthlySales;
    }
}
