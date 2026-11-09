package com.example.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotBlank String productId,
        @NotNull @Positive Integer quantity,
        @NotNull @Valid ShippingAddress shippingAddress) {

    public record ShippingAddress(
            @NotBlank String street,
            @NotBlank String city,
            @NotBlank String zipCode) {
    }
}
