package com.bank.transactionservice;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
public class AccountClient {
    private final RestTemplate restTemplate = new RestTemplate();

    public Map getAccountByNo(String accountNo) {
        return restTemplate.getForObject("http://localhost:8082/accounts/" + accountNo, Map.class);
    }

    public void updateAccount(Map<String, Object> account) {
        restTemplate.put("http://localhost:8082/accounts/update", account);
    }
}
