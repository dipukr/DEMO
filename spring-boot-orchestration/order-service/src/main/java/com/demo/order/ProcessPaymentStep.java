package com.example.order.saga.steps;

import com.example.order.client.PaymentClient;
import com.example.order.client.PaymentClient.PaymentRequest;
import com.example.order.client.PaymentClient.PaymentResponse;
import com.example.order.model.Order;
import com.example.order.saga.SagaStep;
import org.springframework.stereotype.Component;

/** Step 2: charge the customer. Compensation: refund the payment. */
@Component
public class ProcessPaymentStep implements SagaStep {

    private final PaymentClient paymentClient;

    public ProcessPaymentStep(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @Override
    public String name() {
        return "PROCESS_PAYMENT";
    }

    @Override
    public void execute(Order order) {
        PaymentResponse payment = paymentClient.charge(
                new PaymentRequest(order.getId(), order.getCustomerId(), order.getTotalAmount()));
        order.setPaymentId(payment.paymentId());
    }

    @Override
    public void compensate(Order order) {
        paymentClient.refund(order.getId());
    }
}
