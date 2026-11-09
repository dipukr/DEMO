package com.example.order.saga.steps;

import com.example.order.client.ShippingClient;
import com.example.order.client.ShippingClient.ShipmentRequest;
import com.example.order.client.ShippingClient.ShipmentResponse;
import com.example.order.model.Order;
import com.example.order.saga.SagaStep;
import org.springframework.stereotype.Component;

/** Step 3: create the shipment. Compensation: cancel the shipment. */
@Component
public class CreateShipmentStep implements SagaStep {

    private final ShippingClient shippingClient;

    public CreateShipmentStep(ShippingClient shippingClient) {
        this.shippingClient = shippingClient;
    }

    @Override
    public String name() {
        return "CREATE_SHIPMENT";
    }

    @Override
    public void execute(Order order) {
        ShipmentResponse shipment = shippingClient.createShipment(new ShipmentRequest(
                order.getId(), order.getProductId(), order.getQuantity(),
                order.getStreet(), order.getCity(), order.getZipCode()));
        order.setTrackingNumber(shipment.trackingNumber());
    }

    /**
     * Shipping is the last step, so this only runs when the shipping call itself ended with an
     * unknown outcome (e.g. a timeout), or once more steps are added after it.
     */
    @Override
    public void compensate(Order order) {
        shippingClient.cancel(order.getId());
    }
}
