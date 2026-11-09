package com.example.order.saga;

/**
 * Thrown when a participant definitely did NOT apply a command: it rejected it (4xx), or the
 * request never reached it (connection refused). The step therefore needs no compensation.
 */
public class StepFailedException extends RuntimeException {

    public StepFailedException(String message) {
        super(message);
    }
}
