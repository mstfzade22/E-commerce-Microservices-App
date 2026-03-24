package com.ecommerce.productservice.entity;

public enum StockStatus {
    AVAILABLE,
    LOW_STOCK,
    OUT_OF_STOCK;

    public static StockStatus fromQuantity(int quantity) {
        if (quantity <= 0) {
            return OUT_OF_STOCK;
        } else if (quantity <= 10) {
            return LOW_STOCK;
        } else {
            return AVAILABLE;
        }
    }
}
