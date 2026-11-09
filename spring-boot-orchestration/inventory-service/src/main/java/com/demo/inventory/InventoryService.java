package com.example.inventory.service;

import com.example.inventory.dto.ReservationResponse;
import com.example.inventory.dto.ReserveStockRequest;
import com.example.inventory.dto.StockResponse;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ProductNotFoundException;
import com.example.inventory.model.InventoryItem;
import com.example.inventory.model.StockReservation;
import com.example.inventory.repository.InventoryItemRepository;
import com.example.inventory.repository.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository itemRepository;
    private final StockReservationRepository reservationRepository;

    public InventoryService(InventoryItemRepository itemRepository,
                            StockReservationRepository reservationRepository) {
        this.itemRepository = itemRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Saga command: reserve stock for an order, as one local transaction.
     * Idempotent: replaying the command for the same order returns the existing reservation.
     */
    @Transactional
    public ReservationResponse reserve(ReserveStockRequest request) {
        Optional<StockReservation> existing = reservationRepository.findByOrderId(request.orderId());
        if (existing.isPresent()) {
            log.info("Order {}: duplicate reserve command, returning the existing reservation", request.orderId());
            InventoryItem item = itemRepository.findById(existing.get().getProductId()).orElseThrow();
            return ReservationResponse.from(existing.get(), item.getUnitPrice());
        }

        InventoryItem item = itemRepository.findForUpdate(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));

        if (item.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException(item.getProductId(), request.quantity(),
                    item.getAvailableQuantity());
        }

        item.decreaseStock(request.quantity()); // managed entity: written on commit
        StockReservation reservation = reservationRepository.save(
                new StockReservation(request.orderId(), item.getProductId(), request.quantity()));

        log.info("Order {}: reserved {} x {} ({} left)",
                request.orderId(), request.quantity(), item.getProductId(), item.getAvailableQuantity());
        return ReservationResponse.from(reservation, item.getUnitPrice());
    }

    /**
     * Compensating transaction: put the reserved stock back.
     * Idempotent: does nothing if there is no reservation or it was already released.
     */
    @Transactional
    public void release(Long orderId) {
        Optional<StockReservation> found = reservationRepository.findByOrderId(orderId);
        if (found.isEmpty() || found.get().getStatus() == StockReservation.Status.RELEASED) {
            log.info("Order {}: nothing to release", orderId);
            return;
        }

        StockReservation reservation = found.get();
        InventoryItem item = itemRepository.findForUpdate(reservation.getProductId()).orElseThrow();
        item.increaseStock(reservation.getQuantity());
        reservation.release();

        log.info("Order {}: released {} x {} ({} available)",
                orderId, reservation.getQuantity(), item.getProductId(), item.getAvailableQuantity());
    }

    @Transactional(readOnly = true)
    public List<StockResponse> findAll() {
        return itemRepository.findAll().stream().map(StockResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StockResponse find(String productId) {
        return itemRepository.findById(productId)
                .map(StockResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
