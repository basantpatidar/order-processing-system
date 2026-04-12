package com.basant.orderservice;

import com.basant.orderservice.domain.Order;
import com.basant.orderservice.domain.OrderStatus;
import com.basant.orderservice.dto.OrderDtos.*;
import com.basant.orderservice.exception.OrderNotFoundException;
import com.basant.orderservice.kafka.OrderCreatedEvent;
import com.basant.orderservice.kafka.OrderEventProducer;
import com.basant.orderservice.repository.OrderRepository;
import com.basant.orderservice.service.OrderService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer eventProducer;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest validRequest;

    @BeforeEach
    void setUp() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("prod-001");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("49.99"));

        validRequest = new CreateOrderRequest();
        validRequest.setCustomerId("cust-123");
        validRequest.setItems(List.of(item));
    }

    @Test
    @DisplayName("createOrder - persists order and publishes Kafka event")
    void createOrder_shouldPersistAndPublishEvent() {
        // Arrange
        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .customerId("cust-123")
                .status(OrderStatus.PAYMENT_PROCESSING)
                .totalAmount(new BigDecimal("99.98"))
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        OrderResponse response = orderService.createOrder(validRequest);

        // Assert — order was saved twice (initial save + status update)
        verify(orderRepository, times(2)).save(any(Order.class));

        // Assert — Kafka event was published with correct data
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventProducer, times(1)).publishOrderCreated(eventCaptor.capture());

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getCustomerId()).isEqualTo("cust-123");
        assertThat(capturedEvent.getTotalAmount()).isEqualByComparingTo("99.98");

        // Assert — response maps correctly
        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo("cust-123");
    }

    @Test
    @DisplayName("createOrder - computes total amount correctly from items")
    void createOrder_shouldComputeTotalCorrectly() {
        // 2 items x $49.99 = $99.98
        Order captured = Order.builder()
                .id(UUID.randomUUID())
                .customerId("cust-123")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("99.98"))
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(captured);

        OrderResponse response = orderService.createOrder(validRequest);

        assertThat(response.getTotalAmount()).isEqualByComparingTo("99.98");
    }

    @Test
    @DisplayName("getOrder - returns order when found")
    void getOrder_shouldReturnOrder_whenExists() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .customerId("cust-123")
                .status(OrderStatus.PAYMENT_SUCCESS)
                .totalAmount(new BigDecimal("99.98"))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(orderId);

        assertThat(response.getId()).isEqualTo(orderId.toString());
        assertThat(response.getStatus()).isEqualTo("PAYMENT_SUCCESS");
    }

    @Test
    @DisplayName("getOrder - throws OrderNotFoundException when not found")
    void getOrder_shouldThrow_whenNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    @DisplayName("createOrder - does not publish event if DB save fails")
    void createOrder_shouldNotPublishEvent_whenSaveFails() {
        when(orderRepository.save(any(Order.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

        assertThatThrownBy(() -> orderService.createOrder(validRequest))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(eventProducer);
    }
}
