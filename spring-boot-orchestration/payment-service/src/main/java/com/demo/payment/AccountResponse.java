package com.example.payment.dto;

import com.example.payment.model.CustomerAccount;

import java.math.BigDecimal;

public record AccountResponse(String customerId, BigDecimal balance) {

    public static AccountResponse from(CustomerAccount account) {
        return new AccountResponse(account.getCustomerId(), account.getBalance());
    }
}
