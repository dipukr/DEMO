package com.example.payment.repository;

import com.example.payment.model.CustomerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, String> {

    /** SELECT ... FOR UPDATE, so concurrent charges cannot overdraw the account. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CustomerAccount a where a.customerId = :customerId")
    Optional<CustomerAccount> findForUpdate(@Param("customerId") String customerId);
}
