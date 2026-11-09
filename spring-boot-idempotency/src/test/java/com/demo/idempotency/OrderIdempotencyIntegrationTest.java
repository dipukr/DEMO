package com.demo.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs against the SQL Server
// configured in application.properties
@SpringBootTest
@AutoConfigureMockMvc
class OrderIdempotencyIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderService service;

    @Test
    void duplicateRequestReturnsOriginalResultWithoutAnotherOrder() throws Exception {
        String key = UUID.randomUUID().toString();
        String request = """
            {"customerId":"C123","productId":"IPHONE-17","quantity":2}
            """;

        var first = mvc.perform(post("/api/orders")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated()).andReturn().getResponse();
        var second = mvc.perform(post("/api/orders")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated()).andReturn().getResponse();

        assertThat(second.getContentAsString()).isEqualTo(first.getContentAsString());
        assertThat(second.getHeader("Location")).isEqualTo(first.getHeader("Location"));
        assertThat(first.getHeader("Idempotency-Replayed")).isEqualTo("false");
        assertThat(second.getHeader("Idempotency-Replayed")).isEqualTo("true");
        long orderId = mapper.readTree(first.getContentAsString()).get("orderId").asLong();
        assertThat(orderRepository.existsById(orderId)).isTrue();
    }

    @Test
    void reusingKeyWithDifferentPayloadIsConflict() throws Exception {
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/api/orders").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"customerId":"C123","productId":"IPHONE-17","quantity":1}
                    """))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/orders").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"customerId":"C123","productId":"IPHONE-17","quantity":2}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void concurrentRequestsWithSameKeyCreateOnlyOneOrder() throws Exception {
        String key = UUID.randomUUID().toString();
        OrderRequest request = new OrderRequest("C999", "PHONE", 1);
        int count = 8;
        CountDownLatch go = new CountDownLatch(1);
        List<Callable<OrderResult>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(() -> {
                go.await();
                return service.create(key, request);
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(count);
        try {
            List<Future<OrderResult>> futures = new ArrayList<>();
            for (Callable<OrderResult> task : tasks) futures.add(executor.submit(task));
            go.countDown();

            List<OrderResult> results = new ArrayList<>();
            for (Future<OrderResult> future : futures) results.add(future.get());
            Set<Long> orderIds = results.stream().map(OrderResult::getOrderId)
                .collect(Collectors.toSet());
            assertThat(orderIds).hasSize(1);
            assertThat(results.stream().filter(r -> !r.isReplayed()).count()).isEqualTo(1);
            assertThat(orderRepository.existsById(orderIds.iterator().next())).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
