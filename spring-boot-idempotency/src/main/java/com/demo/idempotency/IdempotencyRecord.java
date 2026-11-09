package com.demo.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "idempotency_records")
@IdClass(IdempotencyRecordId.class)
public class IdempotencyRecord {
	
    @Id
    @Column(length = 80)
    private String operation;

    @Id
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "resource_id")
    private Long resourceId;
}
