package com.demo.idempotency;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
public class IdempotencyApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdempotencyApplication.class, args);
    }
    
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        Converter<OrderStatus, String> statusToName =
            ctx -> ctx.getSource() == null ? null : ctx.getSource().name();
        modelMapper.typeMap(OrderEntity.class, OrderResponse.class)
            .addMappings(m -> {
                m.map(OrderEntity::getId, OrderResponse::setOrderId);
                m.using(statusToName).map(OrderEntity::getStatus, OrderResponse::setStatus);
            });
        modelMapper.validate();
        return modelMapper;
    }
}
