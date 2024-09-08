package com.basant.paymentservice.service;

import com.basant.paymentservice.domain.Payment;
import com.basant.paymentservice.domain.PaymentStatus;
import com.basant.paymentservice.dto.OrderCreatedEventDto;
import com.basant.paymentservice.kafka.PaymentProcessedEvent;
import com.basant.paymentservice.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final Random random = new Random();

    @Value("${kafka.topics.payment-processed}")
    private String paymentProcessedTopic;

    /**
     * Processes payment for an order.
     *
     * Idempotency: checks if orderId already processed before doing anything.
     * This handles Kafka at-least-once delivery — if the consumer crashes
     * after DB write but before offset commit, the redelivered message is
     * safely skipped.
     *
     * Simulated payment: 90% success rate to demonstrate failure path + DLQ.
     */
    @Transactional
    public void processPayment(OrderCreatedEventDto event) {
        String orderId = event.getOrderId();

        // ── Idempotency check ─────────────────────────────────────────────────
        if (paymentRepository.existsByOrderId(orderId)) {
            log.warn("[PaymentService] Duplicate message — orderId={} already processed. Skipping.", orderId);
            meterRegistry.counter("payments.duplicate.skipped").increment();
            return;
        }

        log.info("[PaymentService] Processing payment for orderId={}, amount={}",
                orderId, event.getTotalAmount());

        // ── Simulate payment gateway call (90% success) ───────────────────────
        boolean paymentSuccess = random.nextInt(10) < 9;

        Payment payment = Payment.builder()
                .orderId(orderId)
                .customerId(event.getCustomerId())
                .amount(event.getTotalAmount())
                .status(paymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .failureReason(paymentSuccess ? null : "Insufficient funds (simulated)")
                .build();

        Payment saved = paymentRepository.save(payment);

        // ── Publish result event ──────────────────────────────────────────────
        PaymentProcessedEvent resultEvent = PaymentProcessedEvent.builder()
                .paymentId(saved.getId().toString())
                .orderId(orderId)
                .customerId(event.getCustomerId())
                .amount(event.getTotalAmount())
                .status(saved.getStatus().name())
                .failureReason(saved.getFailureReason())
                .processedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(paymentProcessedTopic, orderId, resultEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[PaymentService] Failed to publish PaymentProcessedEvent for orderId={}", orderId, ex);
                    } else {
                        log.info("[PaymentService] Published PaymentProcessedEvent: orderId={}, status={}",
                                orderId, saved.getStatus());
                    }
                });

        meterRegistry.counter("payments.processed",
                "status", saved.getStatus().name()).increment();
    }
}
