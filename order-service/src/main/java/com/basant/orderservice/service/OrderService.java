package com.basant.orderservice.service;

import com.basant.orderservice.domain.Order;
import com.basant.orderservice.domain.OrderItem;
import com.basant.orderservice.domain.OrderStatus;
import com.basant.orderservice.dto.OrderDtos.*;
import com.basant.orderservice.exception.OrderNotFoundException;
import com.basant.orderservice.kafka.OrderCreatedEvent;
import com.basant.orderservice.kafka.OrderEventProducer;
import com.basant.orderservice.repository.OrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;
    private final MeterRegistry meterRegistry;

    /**
     * Creates an order, persists it, then publishes an OrderCreatedEvent.
     * The DB write and Kafka publish are NOT in the same distributed transaction —
     * in production, use the Outbox Pattern or Kafka Transactions for exactly-once
     * end-to-end semantics.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("[OrderService] Creating order for customerId={}", request.getCustomerId());

        // 1. Build domain object
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        // 2. Map items and compute total
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .quantity(itemReq.getQuantity())
                    .price(itemReq.getPrice())
                    .build();
            order.addItem(item);
            total = total.add(itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }
        order.setTotalAmount(total);

        // 3. Persist
        Order saved = orderRepository.save(order);
        log.info("[OrderService] Order persisted: orderId={}, total={}", saved.getId(), saved.getTotalAmount());

        // 4. Update status to PAYMENT_PROCESSING before publishing
        saved.setStatus(OrderStatus.PAYMENT_PROCESSING);
        orderRepository.save(saved);

        // 5. Publish event to Kafka
        OrderCreatedEvent event = buildEvent(saved);
        eventProducer.publishOrderCreated(event);

        // 6. Metrics
        meterRegistry.counter("orders.created",
                "customerId", request.getCustomerId()).increment();

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private OrderCreatedEvent buildEvent(Order order) {
        List<OrderCreatedEvent.OrderItemEvent> itemEvents = order.getItems().stream()
                .map(i -> OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .build())
                .toList();

        return OrderCreatedEvent.builder()
                .orderId(order.getId().toString())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .items(itemEvents)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId().toString());
        response.setCustomerId(order.getCustomerId());
        response.setStatus(order.getStatus().name());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);

        List<OrderItemResponse> itemResponses = order.getItems().stream().map(item -> {
            OrderItemResponse ir = new OrderItemResponse();
            ir.setProductId(item.getProductId());
            ir.setQuantity(item.getQuantity());
            ir.setPrice(item.getPrice());
            ir.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return ir;
        }).toList();

        response.setItems(itemResponses);
        return response;
    }
}
