package com.example.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReserveStockRequest(
        @NotNull Long orderId,
        @NotBlank String productId,
        @Positive int quantity) {
}
