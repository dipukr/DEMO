package com.bank.transactionservice;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class Transaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String type;
	private String fromAccount;
	private String toAccount; // can be null for deposit/withdraw
	private Double amount;
	private LocalDateTime timestamp = LocalDateTime.now();

}
