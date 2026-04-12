package com.basant.orderservice.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    /**
     * Publishes an OrderCreatedEvent to Kafka.
     * Uses orderId as the message key to guarantee same-order events land
     * on the same partition (preserving ordering per order).
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
                kafkaTemplate.send(orderCreatedTopic, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            sample.stop(Timer.builder("kafka.producer.send")
                    .tag("topic", orderCreatedTopic)
                    .tag("status", ex == null ? "success" : "failure")
                    .register(meterRegistry));

            if (ex != null) {
                log.error("[OrderEventProducer] Failed to publish OrderCreatedEvent for orderId={}: {}",
                        event.getOrderId(), ex.getMessage(), ex);
                meterRegistry.counter("kafka.producer.errors", "topic", orderCreatedTopic).increment();
            } else {
                log.info("[OrderEventProducer] Published OrderCreatedEvent: orderId={}, partition={}, offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
