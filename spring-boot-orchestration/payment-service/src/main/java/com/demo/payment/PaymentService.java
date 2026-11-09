package com.example.payment.service;

import com.example.payment.dto.AccountResponse;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.exception.AccountNotFoundException;
import com.example.payment.exception.InsufficientFundsException;
import com.example.payment.model.CustomerAccount;
import com.example.payment.model.Payment;
import com.example.payment.repository.CustomerAccountRepository;
import com.example.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final CustomerAccountRepository accountRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(CustomerAccountRepository accountRepository, PaymentRepository paymentRepository) {
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Saga command: charge the customer, as one local transaction.
     * Idempotent: replaying the command for the same order returns the existing payment.
     */
    @Transactional
    public PaymentResponse charge(PaymentRequest request) {
        Optional<Payment> existing = paymentRepository.findByOrderId(request.orderId());
        if (existing.isPresent()) {
            log.info("Order {}: duplicate charge command, returning the existing payment", request.orderId());
            return PaymentResponse.from(existing.get());
        }

        CustomerAccount account = accountRepository.findForUpdate(request.customerId())
                .orElseThrow(() -> new AccountNotFoundException(request.customerId()));

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(request.customerId(), request.amount(), account.getBalance());
        }

        account.debit(request.amount()); // managed entity: written on commit
        Payment payment = paymentRepository.save(
                new Payment(request.orderId(), request.customerId(), request.amount()));

        log.info("Order {}: charged {} to {} (balance now {})",
                request.orderId(), request.amount(), request.customerId(), account.getBalance());
        return PaymentResponse.from(payment);
    }

    /**
     * Compensating transaction: give the money back.
     * Idempotent: does nothing if there is no payment or it was already refunded.
     */
    @Transactional
    public void refund(Long orderId) {
        Optional<Payment> found = paymentRepository.findByOrderId(orderId);
        if (found.isEmpty() || found.get().getStatus() == Payment.Status.REFUNDED) {
            log.info("Order {}: nothing to refund", orderId);
            return;
        }

        Payment payment = found.get();
        CustomerAccount account = accountRepository.findForUpdate(payment.getCustomerId()).orElseThrow();
        account.credit(payment.getAmount());
        payment.refund();

        log.info("Order {}: refunded {} to {} (balance now {})",
                orderId, payment.getAmount(), payment.getCustomerId(), account.getBalance());
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResponse> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String customerId) {
        return accountRepository.findById(customerId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException(customerId));
    }
}
