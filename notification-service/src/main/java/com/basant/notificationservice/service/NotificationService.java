package com.basant.notificationservice.service;

import com.basant.notificationservice.domain.Notification;
import com.basant.notificationservice.domain.Notification.NotificationType;
import com.basant.notificationservice.dto.EventDtos.OrderCreatedEventDto;
import com.basant.notificationservice.dto.EventDtos.PaymentProcessedEventDto;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MeterRegistry meterRegistry;

    /**
     * Handles order.created — notifies customer that order was received.
     */
    public void handleOrderCreated(OrderCreatedEventDto event) {
        String message = String.format(
                "Hi customer %s, your order %s for $%.2f has been received and is being processed.",
                event.getCustomerId(), event.getOrderId(), event.getTotalAmount()
        );

        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .customerId(event.getCustomerId())
                .orderId(event.getOrderId())
                .type(NotificationType.ORDER_RECEIVED)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();

        send(notification);
    }

    /**
     * Handles payment.processed — notifies customer of payment outcome.
     */
    public void handlePaymentProcessed(PaymentProcessedEventDto event) {
        boolean success = "SUCCESS".equalsIgnoreCase(event.getStatus());

        String message = success
                ? String.format("Payment of $%.2f for order %s was successful! Your order is confirmed.",
                        event.getAmount(), event.getOrderId())
                : String.format("Payment of $%.2f for order %s failed. Reason: %s. Please retry.",
                        event.getAmount(), event.getOrderId(), event.getFailureReason());

        NotificationType type = success
                ? NotificationType.PAYMENT_SUCCESS
                : NotificationType.PAYMENT_FAILED;

        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .customerId(event.getCustomerId())
                .orderId(event.getOrderId())
                .type(type)
                .message(message)
                .sentAt(LocalDateTime.now())
                .build();

        send(notification);
    }

    /**
     * Simulates sending a notification.
     * Replace with real provider: SendGrid (email), Twilio (SMS), AWS SNS, Firebase (push).
     */
    private void send(Notification notification) {
        // Simulate delivery latency
        log.info("[NotificationService] [{}] Sending to customer={}: {}",
                notification.getType(),
                notification.getCustomerId(),
                notification.getMessage());

        // In production: emailClient.send(...) or smsClient.send(...)
        log.info("[NotificationService] [SIMULATED] Notification delivered: id={}, type={}, orderId={}",
                notification.getId(), notification.getType(), notification.getOrderId());

        meterRegistry.counter("notifications.sent",
                "type", notification.getType().name()).increment();
    }
}
