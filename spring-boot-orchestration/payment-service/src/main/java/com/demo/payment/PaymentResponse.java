package com.example.payment.dto;

import com.example.payment.model.Payment;

import java.math.BigDecimal;

public record PaymentResponse(Long paymentId, Long orderId, BigDecimal amount, String status) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(),
                payment.getStatus().name());
    }
}
