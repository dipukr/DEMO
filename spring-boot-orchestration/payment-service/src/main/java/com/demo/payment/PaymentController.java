package com.example.payment.controller;

import com.example.payment.dto.AccountResponse;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Saga command (sent by the orchestrator). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse charge(@Valid @RequestBody PaymentRequest request) {
        return paymentService.charge(request);
    }

    /** Compensating action (sent by the orchestrator). */
    @PostMapping("/{orderId}/refund")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refund(@PathVariable Long orderId) {
        paymentService.refund(orderId);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long orderId) {
        return ResponseEntity.of(paymentService.findByOrderId(orderId));
    }

    @GetMapping("/accounts/{customerId}")
    public AccountResponse getAccount(@PathVariable String customerId) {
        return paymentService.getAccount(customerId);
    }
}
