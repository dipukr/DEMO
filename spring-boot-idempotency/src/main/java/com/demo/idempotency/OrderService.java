package com.demo.idempotency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private static final String OPERATION = "POST:/api/orders";

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResult create(String key, OrderRequest request) {
        validateKey(key);
        String fingerprint = commonUtils.fingerprint(request);
        IdempotencyRecordId recordId = new IdempotencyRecordId(OPERATION, key);

        // First transaction to insert (per key) acquires ownership.
        // Concurrent inserts for that key wait for the first transaction's outcome.
        int inserted = idempotencyRepository.insertIfAbsent(OPERATION, key, fingerprint);

        if (inserted == 0) {
            IdempotencyRecord saved = idempotencyRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Committed idempotency record was not found"));
            if (!saved.getRequestHash().equals(fingerprint))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key was already used with different request data");
            if (saved.getResponseBody() == null || saved.getResourceId() == null)
                throw new IllegalStateException("Incomplete committed idempotency record");
            return new OrderResult(saved.getHttpStatus(), saved.getResponseBody(), saved.getResourceId(), true);
        }

        OrderEntity order = new OrderEntity();
        order.setCustomerId(request.getCustomerId());
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.CREATED);
        Long orderId = orderRepository.save(order).getId();

        OrderResponse response = new OrderResponse();
        response.setOrderId(orderId);
        response.setCustomerId(request.getCustomerId());
        response.setProductId(request.getProductId());
        response.setQuantity(request.getQuantity());
        response.setStatus(OrderStatus.CREATED.name());
        String body = commonUtils.toJson(response);

        // Managed entity: the changes are flushed as an UPDATE at commit.
        IdempotencyRecord record = idempotencyRepository.findById(recordId)
            .orElseThrow(() -> new IllegalStateException("Could not save the idempotent response"));
        record.setHttpStatus(201);
        record.setResponseBody(body);
        record.setResourceId(orderId);
        return new OrderResult(201, body, orderId, false);
    }

    public void validateKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{8,128}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must be 8-128 ASCII letters, numbers, '-' or '_'");
    }
}
