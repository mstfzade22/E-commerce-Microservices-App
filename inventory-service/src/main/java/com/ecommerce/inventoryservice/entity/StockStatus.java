package com.ecommerce.inventoryservice.entity;

public enum StockStatus {
    AVAILABLE,
    LOW_STOCK,
    OUT_OF_STOCK;

    public static StockStatus calculateStatus(int quantity, int lowStockThreshold) {
        if (quantity <= 0) {
            return OUT_OF_STOCK;
        } else if (quantity <= lowStockThreshold) {
            return LOW_STOCK;
        } else {
            return AVAILABLE;
        }
    }
}