package com.example.inventory.dto;

import com.example.inventory.model.InventoryItem;

import java.math.BigDecimal;

public record StockResponse(String productId, String name, int availableQuantity, BigDecimal unitPrice) {

    public static StockResponse from(InventoryItem item) {
        return new StockResponse(item.getProductId(), item.getName(),
                item.getAvailableQuantity(), item.getUnitPrice());
    }
}
