package com.example.order.saga;

import com.example.order.client.InventoryClient;
import com.example.order.client.InventoryClient.ReservationResponse;
import com.example.order.client.PaymentClient;
import com.example.order.client.PaymentClient.PaymentResponse;
import com.example.order.client.ShippingClient;
import com.example.order.client.ShippingClient.ShipmentResponse;
import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.SagaStepLogRepository;
import com.example.order.saga.steps.CreateShipmentStep;
import com.example.order.saga.steps.ProcessPaymentStep;
import com.example.order.saga.steps.ReserveInventoryStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the orchestration logic with the participants mocked out: no services need to run.
 */
@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    private static final long ORDER_ID = 42L;

    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private ShippingClient shippingClient;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private SagaStepLogRepository sagaStepLogRepository;

    private OrderSagaOrchestrator orchestrator;
    private Order order;

    @BeforeEach
    void setUp() {
        orchestrator = new OrderSagaOrchestrator(
                new ReserveInventoryStep(inventoryClient),
                new ProcessPaymentStep(paymentClient),
                new CreateShipmentStep(shippingClient),
                orderRepository,
                sagaStepLogRepository,
                2,
                Duration.ZERO);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        order = new Order("C-1", "P-100", 2, "1 Demo Street", "Springfield", "12345");
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
    }

    @Test
    void completesOrderWhenEveryStepSucceeds() {
        inventoryReservesStock();
        paymentSucceeds();
        when(shippingClient.createShipment(any()))
                .thenReturn(new ShipmentResponse(5L, ORDER_ID, "TRK-1", "CREATED"));

        Order result = orchestrator.execute(order);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.getPaymentId()).isEqualTo(7L);
        assertThat(result.getTrackingNumber()).isEqualTo("TRK-1");
        verify(inventoryClient, never()).release(anyLong());
        verify(paymentClient, never()).refund(anyLong());
    }

    @Test
    void releasesStockWhenPaymentIsDeclined() {
        inventoryReservesStock();
        when(paymentClient.charge(any()))
                .thenThrow(new StepFailedException("Payment Service rejected the request: insufficient funds"));

        Order result = orchestrator.execute(order);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.getFailureReason()).contains("insufficient funds");
        verify(inventoryClient).release(ORDER_ID);
        verify(paymentClient, never()).refund(anyLong());
        verify(shippingClient, never()).createShipment(any());
    }

    @Test
    void compensatesInReverseOrderWhenShippingIsRejected() {
        inventoryReservesStock();
        paymentSucceeds();
        when(shippingClient.createShipment(any()))
                .thenThrow(new StepFailedException("Shipping Service rejected the request"));

        Order result = orchestrator.execute(order);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        InOrder inOrder = inOrder(paymentClient, inventoryClient);
        inOrder.verify(paymentClient).refund(ORDER_ID);
        inOrder.verify(inventoryClient).release(ORDER_ID);
        verify(shippingClient, never()).cancel(anyLong());
    }

    @Test
    void alsoCompensatesAStepWhoseOutcomeIsUnknown() {
        inventoryReservesStock();
        paymentSucceeds();
        when(shippingClient.createShipment(any())).thenThrow(new ResourceAccessException("Read timed out"));

        Order result = orchestrator.execute(order);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        InOrder inOrder = inOrder(shippingClient, paymentClient, inventoryClient);
        inOrder.verify(shippingClient).cancel(ORDER_ID);
        inOrder.verify(paymentClient).refund(ORDER_ID);
        inOrder.verify(inventoryClient).release(ORDER_ID);
    }

    @Test
    void flagsOrderWhenACompensationKeepsFailing() {
        inventoryReservesStock();
        paymentSucceeds();
        when(shippingClient.createShipment(any()))
                .thenThrow(new StepFailedException("Shipping Service rejected the request"));
        doThrow(new ResourceAccessException("Payment Service down")).when(paymentClient).refund(ORDER_ID);

        Order result = orchestrator.execute(order);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPENSATION_FAILED);
        verify(paymentClient, times(2)).refund(ORDER_ID); // retried up to max-attempts
        verify(inventoryClient).release(ORDER_ID);        // remaining compensations still run
    }

    private void inventoryReservesStock() {
        when(inventoryClient.reserve(any())).thenReturn(
                new ReservationResponse(1L, ORDER_ID, "P-100", 2, new BigDecimal("25.00"), "RESERVED"));
    }

    private void paymentSucceeds() {
        when(paymentClient.charge(any())).thenReturn(
                new PaymentResponse(7L, ORDER_ID, new BigDecimal("50.00"), "COMPLETED"));
    }
}
