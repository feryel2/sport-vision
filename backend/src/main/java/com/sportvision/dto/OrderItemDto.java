package com.sportvision.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class OrderItemDto {
    @NotNull(message = "L'ID du produit est requis")
    private Long productId;

    private String productName;

    private String productImageUrl;

    @NotNull(message = "La quantité est requise")
    @Min(value = 1, message = "La quantité doit être d'au moins 1")
    private Integer quantity;

    private BigDecimal unitPrice;

    public OrderItemDto() {}

    public OrderItemDto(Long productId, String productName, String productImageUrl, Integer quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
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

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
