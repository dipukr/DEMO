package com.example.inventory.dto;

import com.example.inventory.model.StockReservation;

import java.math.BigDecimal;

public record ReservationResponse(Long reservationId, Long orderId, String productId,
                                  int quantity, BigDecimal unitPrice, String status) {

    public static ReservationResponse from(StockReservation reservation, BigDecimal unitPrice) {
        return new ReservationResponse(reservation.getId(), reservation.getOrderId(),
                reservation.getProductId(), reservation.getQuantity(), unitPrice,
                reservation.getStatus().name());
    }
}
