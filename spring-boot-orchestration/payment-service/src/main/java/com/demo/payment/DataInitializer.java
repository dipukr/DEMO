package com.example.payment.config;

import com.example.payment.model.CustomerAccount;
import com.example.payment.repository.CustomerAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Demo wallets. C-2 has a low balance so you can trigger a payment failure. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final CustomerAccountRepository repository;

    public DataInitializer(CustomerAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        repository.saveAll(List.of(
                new CustomerAccount("C-1", new BigDecimal("1000.00")),
                new CustomerAccount("C-2", new BigDecimal("50.00"))));
    }
}
