package com.basant.paymentservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentProcessedEvent — published to Kafka topic: payment.processed
 * Consumed by notification-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String status;           // SUCCESS or FAILED
    private String failureReason;    // null if SUCCESS
    private LocalDateTime processedAt;
}
