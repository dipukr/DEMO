package com.example.order.config;

import com.example.order.client.InventoryClient;
import com.example.order.client.PaymentClient;
import com.example.order.client.ShippingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Creates one HTTP client per saga participant.
 */
@Configuration
public class ParticipantClientsConfig {

    private final Duration connectTimeout;
    private final Duration readTimeout;

    public ParticipantClientsConfig(@Value("${saga.http.connect-timeout}") Duration connectTimeout,
                                    @Value("${saga.http.read-timeout}") Duration readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    @Bean
    InventoryClient inventoryClient(RestClient.Builder builder,
                                    @Value("${saga.participants.inventory-url}") String baseUrl) {
        return new InventoryClient(restClient(builder, baseUrl));
    }

    @Bean
    PaymentClient paymentClient(RestClient.Builder builder,
                                @Value("${saga.participants.payment-url}") String baseUrl) {
        return new PaymentClient(restClient(builder, baseUrl));
    }

    @Bean
    ShippingClient shippingClient(RestClient.Builder builder,
                                  @Value("${saga.participants.shipping-url}") String baseUrl) {
        return new ShippingClient(restClient(builder, baseUrl));
    }

    /**
     * Every participant call gets explicit timeouts: a hung participant must never block the saga
     * forever. A call that times out reaches the orchestrator as a failure with an unknown outcome.
     */
    private RestClient restClient(RestClient.Builder builder, String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
