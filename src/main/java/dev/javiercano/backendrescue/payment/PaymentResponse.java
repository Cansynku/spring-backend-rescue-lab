package dev.javiercano.backendrescue.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID orderId, BigDecimal amount,
                              String providerPaymentId, PaymentStatus status) {
    static PaymentResponse from(PaymentEntity payment, UUID orderId) {
        return new PaymentResponse(payment.getId(), orderId, payment.getAmount(),
                payment.getProviderPaymentId(), payment.getStatus());
    }
}
