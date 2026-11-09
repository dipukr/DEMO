package com.example.order.saga;

import com.example.order.model.Order;
import com.example.order.model.SagaStepLog;
import com.example.order.model.SagaStepLog.Action;
import com.example.order.model.SagaStepLog.Outcome;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.SagaStepLogRepository;
import com.example.order.saga.steps.CreateShipmentStep;
import com.example.order.saga.steps.ProcessPaymentStep;
import com.example.order.saga.steps.ReserveInventoryStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Orchestrator of the "place order" saga.
 *
 * <p>This is the only component that knows the workflow. It sends a command to each participant
 * in turn, persists the outcome of every step and, when a step fails, runs the compensating action
 * of every completed step in reverse order. The participants never talk to each other.
 */
@Component
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);
    private static final int MAX_TEXT_LENGTH = 1000;

    private final List<SagaStep> steps;
    private final OrderRepository orderRepository;
    private final SagaStepLogRepository sagaStepLogRepository;
    private final int compensationAttempts;
    private final Duration compensationBackoff;

    public OrderSagaOrchestrator(ReserveInventoryStep reserveInventory,
                                 ProcessPaymentStep processPayment,
                                 CreateShipmentStep createShipment,
                                 OrderRepository orderRepository,
                                 SagaStepLogRepository sagaStepLogRepository,
                                 @Value("${saga.compensation.max-attempts}") int compensationAttempts,
                                 @Value("${saga.compensation.backoff}") Duration compensationBackoff) {
        // The saga definition: steps run top to bottom, compensations run bottom to top.
        this.steps = List.of(reserveInventory, processPayment, createShipment);
        this.orderRepository = orderRepository;
        this.sagaStepLogRepository = sagaStepLogRepository;
        this.compensationAttempts = compensationAttempts;
        this.compensationBackoff = compensationBackoff;
    }

    public Order execute(Order order) {
        log.info("Order {}: saga started", order.getId());
        Deque<SagaStep> completed = new ArrayDeque<>();

        for (SagaStep step : steps) {
            try {
                step.execute(order);
            } catch (StepFailedException e) {
                // The participant did NOT apply the change, so only the earlier steps need undoing.
                logStep(order, step, Action.EXECUTE, Outcome.FAILED, e.getMessage());
                return compensate(order, completed, e.getMessage());
            } catch (RuntimeException e) {
                // Outcome unknown (read timeout, 5xx...): the participant MAY have applied the change,
                // so undo this step as well. Safe because every compensation is idempotent.
                String reason = step.name() + " outcome unknown: " + e.getMessage();
                logStep(order, step, Action.EXECUTE, Outcome.FAILED, reason);
                completed.push(step);
                return compensate(order, completed, reason);
            }
            completed.push(step);
            order = orderRepository.save(order); // persist data returned by the participant
            logStep(order, step, Action.EXECUTE, Outcome.SUCCESS, null);
        }

        order.markCompleted();
        log.info("Order {}: saga completed", order.getId());
        return orderRepository.save(order);
    }

    private Order compensate(Order order, Deque<SagaStep> completed, String reason) {
        log.warn("Order {}: compensating {} step(s)", order.getId(), completed.size());
        boolean allCompensated = true;

        while (!completed.isEmpty()) {
            SagaStep step = completed.pop(); // LIFO: undo the most recent step first
            try {
                compensateWithRetry(step, order);
                logStep(order, step, Action.COMPENSATE, Outcome.SUCCESS, null);
            } catch (RuntimeException e) {
                // Keep undoing the remaining steps, but flag the order for follow-up.
                allCompensated = false;
                logStep(order, step, Action.COMPENSATE, Outcome.FAILED, e.getMessage());
            }
        }

        if (allCompensated) {
            order.markCancelled(truncate(reason));
        } else {
            order.markCompensationFailed(truncate(reason));
        }
        log.warn("Order {}: saga finished with status {}", order.getId(), order.getStatus());
        return orderRepository.save(order);
    }

    /** A compensation has to succeed eventually, so transient failures are retried with backoff. */
    private void compensateWithRetry(SagaStep step, Order order) {
        for (int attempt = 1; ; attempt++) {
            try {
                step.compensate(order);
                return;
            } catch (RuntimeException e) {
                if (attempt >= compensationAttempts) {
                    throw e;
                }
                log.warn("Order {}: compensating {} failed (attempt {}/{}): {}",
                        order.getId(), step.name(), attempt, compensationAttempts, e.getMessage());
                sleep(compensationBackoff.multipliedBy(attempt));
            }
        }
    }

    private void logStep(Order order, SagaStep step, Action action, Outcome outcome, String detail) {
        if (outcome == Outcome.SUCCESS) {
            log.info("Order {}: {} {} -> {}", order.getId(), action, step.name(), outcome);
        } else {
            log.warn("Order {}: {} {} -> {} ({})", order.getId(), action, step.name(), outcome, detail);
        }
        sagaStepLogRepository.save(new SagaStepLog(order.getId(), step.name(), action, outcome, truncate(detail)));
    }

    private static String truncate(String text) {
        return text == null || text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while compensating", e);
        }
    }
}
