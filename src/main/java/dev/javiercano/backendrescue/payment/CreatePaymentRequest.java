package dev.javiercano.backendrescue.payment;

import java.math.BigDecimal;

// BR-004: no amount validation or relationship to the order total.
public record CreatePaymentRequest(BigDecimal amount) {}
