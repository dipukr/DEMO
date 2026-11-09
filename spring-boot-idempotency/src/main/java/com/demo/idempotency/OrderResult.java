package com.demo.idempotency;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResult {
    private final int httpStatus;
    private final String responseBody;
    private final Long orderId;
    private final boolean replayed;
}
