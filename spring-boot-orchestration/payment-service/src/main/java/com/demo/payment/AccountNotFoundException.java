package com.example.payment.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String customerId) {
        super("No account for customer " + customerId);
    }
}
