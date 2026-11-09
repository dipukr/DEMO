package com.example.inventory.controller;

import com.example.inventory.dto.ReservationResponse;
import com.example.inventory.dto.ReserveStockRequest;
import com.example.inventory.dto.StockResponse;
import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** Saga command (sent by the orchestrator). */
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Valid @RequestBody ReserveStockRequest request) {
        return inventoryService.reserve(request);
    }

    /** Compensating action (sent by the orchestrator). */
    @PostMapping("/reservations/{orderId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable Long orderId) {
        inventoryService.release(orderId);
    }

    @GetMapping
    public List<StockResponse> findAll() {
        return inventoryService.findAll();
    }

    @GetMapping("/{productId}")
    public StockResponse find(@PathVariable String productId) {
        return inventoryService.find(productId);
    }
}
