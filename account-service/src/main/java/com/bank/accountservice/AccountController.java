package com.bank.accountservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountService service;

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        return ResponseEntity.ok(service.createAccount(account));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<Account> getAccount(@PathVariable String accountNumber) {
        Optional<Account> acc = service.getByAccountNumber(accountNumber);
        return acc.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/update")
    public ResponseEntity<Account> update(@RequestBody Account account) {
        return ResponseEntity.ok(service.update(account));
    }
}
