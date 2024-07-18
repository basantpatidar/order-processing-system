package com.basant.orderservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderCreatedEvent — published to Kafka topic: order.created
 *
 * This is the contract that payment-service and notification-service
 * both consume. In a real system this would live in a shared library.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private List<OrderItemEvent> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private String productId;
        private Integer quantity;
        private BigDecimal price;
    }
}
