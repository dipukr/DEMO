package com.example.order.model;

public enum OrderStatus {

    /** The saga is running. */
    PENDING,

    /** Every step succeeded. */
    COMPLETED,

    /** A step failed and every completed step was compensated. */
    CANCELLED,

    /** A step failed and at least one compensation could not be applied: needs a retry or a manual fix. */
    COMPENSATION_FAILED
}
