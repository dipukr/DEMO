package com.demo.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, IdempotencyRecordId> {

    // Claims the key without raising a duplicate-key error: under JPA a failed
    // statement marks the whole transaction rollback-only, so it cannot be caught.
    // UPDLOCK + HOLDLOCK lock the key (or its gap), so a concurrent claim for the
    // same key waits for this transaction to commit or roll back.
    // Returns 1 when this transaction owns the key, 0 when it already exists.
    @Modifying
    @Query(nativeQuery = true, value = """
        INSERT INTO idempotency_records (operation, idempotency_key, request_hash)
        SELECT :operation, :key, :hash
        WHERE NOT EXISTS (
            SELECT 1 FROM idempotency_records WITH (UPDLOCK, HOLDLOCK)
            WHERE operation = :operation AND idempotency_key = :key)
    """)
    int insertIfAbsent(String operation, String key, String hash);
}
