package com.example.order.saga.steps;

import com.example.order.client.InventoryClient;
import com.example.order.client.InventoryClient.ReservationResponse;
import com.example.order.client.InventoryClient.ReserveStockRequest;
import com.example.order.model.Order;
import com.example.order.saga.SagaStep;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Step 1: reserve stock. Compensation: release the reservation. */
@Component
public class ReserveInventoryStep implements SagaStep {

    private final InventoryClient inventoryClient;

    public ReserveInventoryStep(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @Override
    public String name() {
        return "RESERVE_INVENTORY";
    }

    @Override
    public void execute(Order order) {
        ReservationResponse reservation = inventoryClient.reserve(
                new ReserveStockRequest(order.getId(), order.getProductId(), order.getQuantity()));

        // The orchestrator carries data from one participant's reply into the next command:
        // the price comes from Inventory and is what the Payment step will charge.
        order.setTotalAmount(reservation.unitPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
    }

    @Override
    public void compensate(Order order) {
        inventoryClient.release(order.getId());
    }
}
