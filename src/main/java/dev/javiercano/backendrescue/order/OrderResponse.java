package dev.javiercano.backendrescue.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(UUID id, String customerEmail, BigDecimal totalAmount,
                            OrderStatus status, int paymentCount) {
    static OrderResponse from(PurchaseOrderEntity order) {
        // BR-005: initializes each lazy collection when mapping a list of orders.
        return new OrderResponse(order.getId(), order.getCustomerEmail(), order.getTotalAmount(),
                order.getStatus(), order.getPayments().size());
    }
}
