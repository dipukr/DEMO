package com.example.payment.model;

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
@Table(name = "payment")
public class Payment {

    public enum Status { COMPLETED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** At most one payment per order: this is what makes the saga command idempotent. */
    @Column(nullable = false, unique = true)
    private Long orderId;

    private String customerId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant createdAt;

    protected Payment() {
        // required by JPA
    }

    public Payment(Long orderId, String customerId, BigDecimal amount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = Status.COMPLETED;
        this.createdAt = Instant.now();
    }

    public void refund() {
        this.status = Status.REFUNDED;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
