package com.demo.idempotency;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IdempotencyRecordId implements Serializable {
    private static final long serialVersionUID = 1L;
	private String operation;
    private String idempotencyKey;
}
