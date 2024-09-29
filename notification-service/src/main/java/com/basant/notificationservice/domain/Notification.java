package com.basant.notificationservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * In-memory notification record.
 * In production this would be persisted to a DB or pushed to
 * an email/SMS provider (SendGrid, Twilio, AWS SNS, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private String customerId;
    private String orderId;
    private NotificationType type;
    private String message;
    private LocalDateTime sentAt;

    public enum NotificationType {
        ORDER_RECEIVED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED
    }
}
