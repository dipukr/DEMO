package com.example.payment.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String customerId, BigDecimal required, BigDecimal available) {
        super("Insufficient funds for customer %s: required %s, available %s"
                .formatted(customerId, required, available));
    }
}
