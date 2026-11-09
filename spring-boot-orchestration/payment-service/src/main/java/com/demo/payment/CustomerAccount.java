package com.example.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_account")
public class CustomerAccount {

    @Id
    private String customerId;

    @Column(precision = 12, scale = 2)
    private BigDecimal balance;

    protected CustomerAccount() {
        // required by JPA
    }

    public CustomerAccount(String customerId, BigDecimal balance) {
        this.customerId = customerId;
        this.balance = balance;
    }

    public void debit(BigDecimal amount) {
        this.balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = balance.add(amount);
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
