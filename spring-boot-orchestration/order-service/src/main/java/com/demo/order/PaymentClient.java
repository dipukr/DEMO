package com.example.order.client;

import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/** HTTP adapter for the Payment Service (saga participant). */
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Command: charge the customer for an order. */
    public PaymentResponse charge(PaymentRequest request) {
        return RemoteCall.command("Payment Service", () -> restClient.post()
                .uri("/api/payments")
                .body(request)
                .retrieve()
                .body(PaymentResponse.class));
    }

    /** Compensation: refund the order's payment. Idempotent on the server side. */
    public void refund(Long orderId) {
        restClient.post()
                .uri("/api/payments/{orderId}/refund", orderId)
                .retrieve()
                .toBodilessEntity();
    }

    public record PaymentRequest(Long orderId, String customerId, BigDecimal amount) {
    }

    public record PaymentResponse(Long paymentId, Long orderId, BigDecimal amount, String status) {
    }
}
