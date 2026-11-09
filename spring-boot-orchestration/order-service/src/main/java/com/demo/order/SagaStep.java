package com.example.order.saga;

import com.example.order.model.Order;

/**
 * One step of the saga: a local transaction executed by a participant service, paired with the
 * compensating transaction that semantically undoes it.
 */
public interface SagaStep {

    /** Name used in logs and in the saga log, e.g. RESERVE_INVENTORY. */
    String name();

    /**
     * Sends the command to the participant. May copy data from the participant's reply onto the
     * order so that later steps can use it.
     *
     * @throws StepFailedException if the participant definitely did not apply the change
     */
    void execute(Order order);

    /**
     * Undoes {@link #execute}. Must be idempotent: it can be retried, and it can run for a step
     * whose outcome is unknown (in which case there may be nothing to undo).
     */
    void compensate(Order order);
}
