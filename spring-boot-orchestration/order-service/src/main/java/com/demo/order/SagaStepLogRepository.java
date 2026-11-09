package com.example.order.repository;

import com.example.order.model.SagaStepLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SagaStepLogRepository extends JpaRepository<SagaStepLog, Long> {

    List<SagaStepLog> findByOrderIdOrderByIdAsc(Long orderId);
}
