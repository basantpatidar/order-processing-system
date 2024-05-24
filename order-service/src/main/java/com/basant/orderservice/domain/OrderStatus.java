package com.basant.orderservice.domain;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    CANCELLED
}
