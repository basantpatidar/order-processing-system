package com.basant.paymentservice;

import com.basant.paymentservice.domain.Payment;
import com.basant.paymentservice.domain.PaymentStatus;
import com.basant.paymentservice.dto.OrderCreatedEventDto;
import com.basant.paymentservice.kafka.PaymentProcessedEvent;
import com.basant.paymentservice.repository.PaymentRepository;
import com.basant.paymentservice.service.PaymentService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;

    private PaymentService paymentService;

    private OrderCreatedEventDto sampleEvent;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, kafkaTemplate, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(paymentService, "paymentProcessedTopic", "payment.processed");

        sampleEvent = OrderCreatedEventDto.builder()
                .orderId("order-001")
                .customerId("cust-123")
                .totalAmount(new BigDecimal("99.98"))
                .items(List.of())
                .build();

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName("processPayment - skips duplicate orderId")
    void processPayment_skipsDuplicate() {
        when(paymentRepository.existsByOrderId("order-001")).thenReturn(true);

        paymentService.processPayment(sampleEvent);

        verifyNoMoreInteractions(paymentRepository);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("processPayment - saves payment and publishes event")
    void processPayment_savesAndPublishes() {
        when(paymentRepository.existsByOrderId("order-001")).thenReturn(false);

        Payment savedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId("order-001")
                .customerId("cust-123")
                .amount(new BigDecimal("99.98"))
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.save(any())).thenReturn(savedPayment);

        paymentService.processPayment(sampleEvent);

        verify(paymentRepository).save(any(Payment.class));

        ArgumentCaptor<PaymentProcessedEvent> captor =
                ArgumentCaptor.forClass(PaymentProcessedEvent.class);
        verify(kafkaTemplate).send(eq("payment.processed"), eq("order-001"), captor.capture());

        PaymentProcessedEvent published = captor.getValue();
        assertThat(published.getOrderId()).isEqualTo("order-001");
        assertThat(published.getCustomerId()).isEqualTo("cust-123");
        assertThat(published.getAmount()).isEqualByComparingTo("99.98");
    }
}
