package com.basant.notificationservice.consumer;

import com.basant.notificationservice.dto.EventDtos.OrderCreatedEventDto;
import com.basant.notificationservice.dto.EventDtos.PaymentProcessedEventDto;
import com.basant.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    /**
     * Listens to order.created — sends "order received" notification.
     * Uses a separate consumer group so this service independently
     * tracks its own offset, decoupled from payment-service.
     */
    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "notification-service-order-group",
            containerFactory = "orderCreatedContainerFactory"
    )
    public void onOrderCreated(ConsumerRecord<String, OrderCreatedEventDto> record) {
        log.info("[NotificationConsumer] order.created received: orderId={}, offset={}",
                record.key(), record.offset());
        try {
            notificationService.handleOrderCreated(record.value());
        } catch (Exception ex) {
            log.error("[NotificationConsumer] Failed to handle order.created for orderId={}: {}",
                    record.key(), ex.getMessage(), ex);
        }
    }

    /**
     * Listens to payment.processed — sends payment success/failure notification.
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-processed}",
            groupId = "notification-service-payment-group",
            containerFactory = "paymentProcessedContainerFactory"
    )
    public void onPaymentProcessed(ConsumerRecord<String, PaymentProcessedEventDto> record) {
        log.info("[NotificationConsumer] payment.processed received: orderId={}, status={}",
                record.key(), record.value() != null ? record.value().getStatus() : "null");
        try {
            notificationService.handlePaymentProcessed(record.value());
        } catch (Exception ex) {
            log.error("[NotificationConsumer] Failed to handle payment.processed for orderId={}: {}",
                    record.key(), ex.getMessage(), ex);
        }
    }
}
