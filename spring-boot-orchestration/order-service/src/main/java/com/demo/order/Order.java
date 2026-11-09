package com.example.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders") // ORDER is a reserved SQL keyword
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;
    private String productId;
    private int quantity;

    private String street;
    private String city;
    private String zipCode;

    // Filled in by the saga from the participants' replies
    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;
    private Long paymentId;
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(length = 1000)
    private String failureReason;

    private Instant createdAt;

    protected Order() {
        // required by JPA
    }

    public Order(String customerId, String productId, int quantity,
                 String street, String city, String zipCode) {
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markCompleted() {
        this.status = OrderStatus.COMPLETED;
    }

    public void markCancelled(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.failureReason = reason;
    }

    public void markCompensationFailed(String reason) {
        this.status = OrderStatus.COMPENSATION_FAILED;
        this.failureReason = reason;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
