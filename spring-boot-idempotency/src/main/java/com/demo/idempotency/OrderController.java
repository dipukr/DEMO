package com.demo.idempotency;

import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
	
    @Autowired
    private OrderService service;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody OrderRequest request) {
        OrderResult result = service.create(idempotencyKey, request);
        return ResponseEntity.status(result.getHttpStatus())
            .contentType(MediaType.APPLICATION_JSON)
            .location(URI.create("/api/orders/" + result.getOrderId()))
            .header("Idempotency-Replayed", Boolean.toString(result.isReplayed()))
            .body(result.getResponseBody());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
            .map(order -> modelMapper.map(order, OrderResponse.class))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
