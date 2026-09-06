package dev.javiercano.backendrescue.order;

import java.math.BigDecimal;

// BR-004: intentionally lacks request validation.
public record CreateOrderRequest(String customerEmail, BigDecimal totalAmount) {}
