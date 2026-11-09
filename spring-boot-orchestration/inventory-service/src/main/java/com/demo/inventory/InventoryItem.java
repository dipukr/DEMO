package com.example.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    private String productId;

    private String name;

    private int availableQuantity;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;

    protected InventoryItem() {
        // required by JPA
    }

    public InventoryItem(String productId, String name, int availableQuantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.name = name;
        this.availableQuantity = availableQuantity;
        this.unitPrice = unitPrice;
    }

    public void decreaseStock(int quantity) {
        this.availableQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        this.availableQuantity += quantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
