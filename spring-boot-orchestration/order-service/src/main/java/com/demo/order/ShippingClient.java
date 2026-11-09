package com.example.order.client;

import org.springframework.web.client.RestClient;

/** HTTP adapter for the Shipping Service (saga participant). */
public class ShippingClient {

    private final RestClient restClient;

    public ShippingClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Command: create a shipment for an order. */
    public ShipmentResponse createShipment(ShipmentRequest request) {
        return RemoteCall.command("Shipping Service", () -> restClient.post()
                .uri("/api/shipments")
                .body(request)
                .retrieve()
                .body(ShipmentResponse.class));
    }

    /** Compensation: cancel the order's shipment. Idempotent on the server side. */
    public void cancel(Long orderId) {
        restClient.post()
                .uri("/api/shipments/{orderId}/cancel", orderId)
                .retrieve()
                .toBodilessEntity();
    }

    public record ShipmentRequest(Long orderId, String productId, int quantity,
                                  String street, String city, String zipCode) {
    }

    public record ShipmentResponse(Long shipmentId, Long orderId, String trackingNumber, String status) {
    }
}
