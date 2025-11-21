package com.datasaz.ecommerce.repositories.entities;

public enum OrderStatus {
    PENDING, // Order is placed but not yet processed
    PENDING_PAYMENT,
    CONFIRMED,
    PAID,
    PAYMENT_FAILED,
    SHIPPED,
    DELIVERED,
    RETURN_REQUESTED,
    RETURN_APPROVED,
    RETURN_REJECTED,
    RETURN_COMPLETED,
    PROCESSING,
    CANCELLED, // Order was cancelled by the customer or seller
    RETURNED, // Product was returned by the customer
    REFUNDED // Payment was refunded to the customer
}
