package com.basant.orderservice.controller;

import com.basant.orderservice.dto.OrderDtos.*;
import com.basant.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/orders
     * Place a new order.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("[OrderController] POST /api/orders for customerId={}", request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/orders/{orderId}
     * Fetch a single order by ID.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    /**
     * GET /api/orders?customerId=xxx
     * Fetch all orders for a customer.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(
            @RequestParam String customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }
}
