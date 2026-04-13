package com.basant.paymentservice.consumer;

import com.basant.paymentservice.dto.OrderCreatedEventDto;
import com.basant.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    /**
     * Consumes order.created events from Kafka.
     *
     * @RetryableTopic configures:
     *   - 3 retry attempts with exponential backoff (1s → 2s → 4s)
     *   - Retries go to: order.created-retry-0, order.created-retry-1, order.created-retry-2
     *   - After all retries exhausted → order.created.DLT (Dead Letter Topic)
     *
     * Manual acknowledgment (MANUAL_IMMEDIATE) ensures offset is only committed
     * after successful processing — preventing silent message loss.
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".DLT",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "true"
    )
    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(ConsumerRecord<String, OrderCreatedEventDto> record,
                               Acknowledgment acknowledgment) {
        log.info("[OrderCreatedConsumer] Received event: topic={}, partition={}, offset={}, orderId={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            paymentService.processPayment(record.value());
            acknowledgment.acknowledge();
            log.info("[OrderCreatedConsumer] Successfully processed orderId={}", record.key());
        } catch (Exception ex) {
            log.error("[OrderCreatedConsumer] Error processing orderId={}: {}",
                    record.key(), ex.getMessage(), ex);
            // Do NOT acknowledge — Spring Retry will redeliver
            throw ex;
        }
    }

    /**
     * Dead Letter Topic handler — called after all retries are exhausted.
     * In production: alert, store for manual replay, trigger incident.
     */
    @KafkaListener(
            topics = "${kafka.topics.order-created}.DLT",
            groupId = "payment-service-dlt-group"
    )
    public void onOrderCreatedDlt(ConsumerRecord<String, OrderCreatedEventDto> record) {
        log.error("[OrderCreatedConsumer][DLT] Message permanently failed after all retries. " +
                "orderId={}, partition={}, offset={}. Manual intervention required.",
                record.key(), record.partition(), record.offset());
        // TODO: alert ops team, write to dead_letters audit table, trigger PagerDuty
    }
}
