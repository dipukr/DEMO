package com.example.inventory.config;

import com.example.inventory.model.InventoryItem;
import com.example.inventory.repository.InventoryItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Demo stock. P-300 is out of stock so you can trigger a failure at the first saga step. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final InventoryItemRepository repository;

    public DataInitializer(InventoryItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        repository.saveAll(List.of(
                new InventoryItem("P-100", "Wireless Mouse", 10, new BigDecimal("25.00")),
                new InventoryItem("P-200", "Mechanical Keyboard", 5, new BigDecimal("80.00")),
                new InventoryItem("P-300", "4K Monitor", 0, new BigDecimal("300.00"))));
    }
}
