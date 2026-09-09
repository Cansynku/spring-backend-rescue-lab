package dev.javiercano.backendrescue.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(UUID id, String customerEmail, BigDecimal totalAmount,
                            OrderStatus status, int paymentCount) {
    public OrderResponse(UUID id, String customerEmail, BigDecimal totalAmount,
                         OrderStatus status, long paymentCount) {
        this(id, customerEmail, totalAmount, status, Math.toIntExact(paymentCount));
    }

    static OrderResponse from(PurchaseOrderEntity order) {
        return new OrderResponse(order.getId(), order.getCustomerEmail(), order.getTotalAmount(),
                order.getStatus(), order.getPayments().size());
    }
}
