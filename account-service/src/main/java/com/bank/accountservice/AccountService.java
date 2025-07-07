package com.bank.accountservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountDAO accountDAO;

    public Account createAccount(Account account) {
        account.setBalance(account.getBalance() != null ? account.getBalance() : 0.0);
        return accountDAO.save(account);
    }

    public Optional<Account> getByAccountNumber(String accountNo) {
        return accountDAO.findByAccountNo(accountNo);
    }

    public Optional<Account> getById(Integer id) {
        return accountDAO.findById(id);
    }

    public Account update(Account account) {
        return accountDAO.save(account);
    }
}

