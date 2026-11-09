package com.example.order.dto;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.model.SagaStepLog;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(
        Long orderId,
        String customerId,
        String productId,
        int quantity,
        BigDecimal totalAmount,
        OrderStatus status,
        String failureReason,
        Long paymentId,
        String trackingNumber,
        List<SagaLogEntry> sagaLog) {

    public static OrderResponse from(Order order, List<SagaStepLog> sagaLog) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getFailureReason(),
                order.getPaymentId(),
                order.getTrackingNumber(),
                sagaLog.stream().map(SagaLogEntry::from).toList());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SagaLogEntry(String step, SagaStepLog.Action action, SagaStepLog.Outcome outcome,
                               String detail, Instant at) {

        static SagaLogEntry from(SagaStepLog entry) {
            return new SagaLogEntry(entry.getStep(), entry.getAction(), entry.getOutcome(),
                    entry.getDetail(), entry.getOccurredAt());
        }
    }
}
