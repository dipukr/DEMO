package com.demo.idempotency;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonPropertyOrder({"orderId", "customerId", "productId", "quantity", "status"})
public class OrderResponse {
    private Long orderId;
    private String customerId;
    private String productId;
    private int quantity;
    private String status;
}
