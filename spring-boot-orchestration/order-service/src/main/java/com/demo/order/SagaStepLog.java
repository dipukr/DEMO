package com.example.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Durable record of what the orchestrator did for an order. Persisting saga progress is what lets
 * an orchestrator explain (and, in production, recover) a saga after the fact.
 */
@Entity
@Table(name = "saga_step_log")
public class SagaStepLog {

    public enum Action { EXECUTE, COMPENSATE }

    public enum Outcome { SUCCESS, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String step;

    @Enumerated(EnumType.STRING)
    private Action action;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    @Column(length = 1000)
    private String detail;

    private Instant occurredAt;

    protected SagaStepLog() {
        // required by JPA
    }

    public SagaStepLog(Long orderId, String step, Action action, Outcome outcome, String detail) {
        this.orderId = orderId;
        this.step = step;
        this.action = action;
        this.outcome = outcome;
        this.detail = detail;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getStep() {
        return step;
    }

    public Action getAction() {
        return action;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
