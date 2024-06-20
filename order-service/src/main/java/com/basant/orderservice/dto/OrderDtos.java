package com.basant.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// ─── Inbound ──────────────────────────────────────────────────────────────

public class OrderDtos {

    @Data
    public static class CreateOrderRequest {
        @NotBlank(message = "customerId is required")
        private String customerId;

        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        private List<OrderItemRequest> items;
    }

    @Data
    public static class OrderItemRequest {
        @NotBlank(message = "productId is required")
        private String productId;

        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;

        @DecimalMin(value = "0.01", message = "price must be greater than 0")
        @NotNull
        private BigDecimal price;
    }

    // ─── Outbound ─────────────────────────────────────────────────────────────

    @Data
    public static class OrderResponse {
        private String id;
        private String customerId;
        private String status;
        private BigDecimal totalAmount;
        private List<OrderItemResponse> items;
        private String createdAt;
    }

    @Data
    public static class OrderItemResponse {
        private String productId;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
    }
}
