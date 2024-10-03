package com.basant.notificationservice;

import com.basant.notificationservice.dto.EventDtos.OrderCreatedEventDto;
import com.basant.notificationservice.dto.EventDtos.PaymentProcessedEventDto;
import com.basant.notificationservice.service.NotificationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("handleOrderCreated - processes without error")
    void handleOrderCreated_doesNotThrow() {
        OrderCreatedEventDto event = new OrderCreatedEventDto(
                "order-001", "cust-123", new BigDecimal("99.98"), null, null);

        assertDoesNotThrow(() -> notificationService.handleOrderCreated(event));
    }

    @Test
    @DisplayName("handlePaymentProcessed - success path")
    void handlePaymentProcessed_success() {
        PaymentProcessedEventDto event = new PaymentProcessedEventDto(
                "pay-001", "order-001", "cust-123",
                new BigDecimal("99.98"), "SUCCESS", null, null);

        assertDoesNotThrow(() -> notificationService.handlePaymentProcessed(event));
    }

    @Test
    @DisplayName("handlePaymentProcessed - failure path includes reason")
    void handlePaymentProcessed_failure() {
        PaymentProcessedEventDto event = new PaymentProcessedEventDto(
                "pay-002", "order-002", "cust-456",
                new BigDecimal("49.99"), "FAILED", "Insufficient funds (simulated)", null);

        assertDoesNotThrow(() -> notificationService.handlePaymentProcessed(event));
    }
}
