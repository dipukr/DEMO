package com.example.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "stock_reservation")
public class StockReservation {

    public enum Status { RESERVED, RELEASED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** At most one reservation per order: this is what makes the saga command idempotent. */
    @Column(nullable = false, unique = true)
    private Long orderId;

    private String productId;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant createdAt;

    protected StockReservation() {
        // required by JPA
    }

    public StockReservation(Long orderId, String productId, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = Status.RESERVED;
        this.createdAt = Instant.now();
    }

    public void release() {
        this.status = Status.RELEASED;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
