package com.example.order.service;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.SagaStepLogRepository;
import com.example.order.saga.OrderSagaOrchestrator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final SagaStepLogRepository sagaStepLogRepository;
    private final OrderSagaOrchestrator orchestrator;

    public OrderService(OrderRepository orderRepository,
                        SagaStepLogRepository sagaStepLogRepository,
                        OrderSagaOrchestrator orchestrator) {
        this.orderRepository = orderRepository;
        this.sagaStepLogRepository = sagaStepLogRepository;
        this.orchestrator = orchestrator;
    }

    /**
     * Deliberately NOT @Transactional. Each saga step is a separate local transaction inside
     * another service; holding a database transaction open across remote calls is exactly what
     * the saga pattern avoids. The order is saved as PENDING first, then the orchestrator takes over.
     */
    public OrderResponse placeOrder(CreateOrderRequest request) {
        CreateOrderRequest.ShippingAddress address = request.shippingAddress();
        Order order = orderRepository.save(new Order(
                request.customerId(),
                request.productId(),
                request.quantity(),
                address.street(),
                address.city(),
                address.zipCode()));

        return toResponse(orchestrator.execute(order));
    }

    public Optional<OrderResponse> findOrder(Long orderId) {
        return orderRepository.findById(orderId).map(this::toResponse);
    }

    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.from(order, sagaStepLogRepository.findByOrderIdOrderByIdAsc(order.getId()));
    }
}
