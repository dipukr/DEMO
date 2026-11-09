package com.example.order.client;

import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/** HTTP adapter for the Inventory Service (saga participant). */
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Command: reserve stock for an order. */
    public ReservationResponse reserve(ReserveStockRequest request) {
        return RemoteCall.command("Inventory Service", () -> restClient.post()
                .uri("/api/inventory/reservations")
                .body(request)
                .retrieve()
                .body(ReservationResponse.class));
    }

    /** Compensation: release the stock reserved for an order. Idempotent on the server side. */
    public void release(Long orderId) {
        restClient.post()
                .uri("/api/inventory/reservations/{orderId}/release", orderId)
                .retrieve()
                .toBodilessEntity();
    }

    public record ReserveStockRequest(Long orderId, String productId, int quantity) {
    }

    public record ReservationResponse(Long reservationId, Long orderId, String productId,
                                      int quantity, BigDecimal unitPrice, String status) {
    }
}
