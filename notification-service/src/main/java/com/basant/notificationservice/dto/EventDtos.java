package com.basant.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class EventDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCreatedEventDto {
        private String orderId;
        private String customerId;
        private BigDecimal totalAmount;
        private List<OrderItemDto> items;
        private LocalDateTime createdAt;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderItemDto {
            private String productId;
            private Integer quantity;
            private BigDecimal price;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentProcessedEventDto {
        private String paymentId;
        private String orderId;
        private String customerId;
        private BigDecimal amount;
        private String status;
        private String failureReason;
        private LocalDateTime processedAt;
    }
}
