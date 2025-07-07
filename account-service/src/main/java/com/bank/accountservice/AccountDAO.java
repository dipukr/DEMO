package com.bank.accountservice;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountDAO extends JpaRepository<Account, Integer> {
	Optional<Account> findByAccountNo(String accountNo);
}
