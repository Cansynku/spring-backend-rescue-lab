package dev.javiercano.backendrescue.payment;

import dev.javiercano.backendrescue.order.OrderRepository;
import dev.javiercano.backendrescue.order.OrderStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class PaymentTransactions {
    private final OrderRepository orders;
    private final PaymentRepository payments;

    public PaymentTransactions(OrderRepository orders, PaymentRepository payments) {
        this.orders = orders;
        this.payments = payments;
    }

    public record Reservation(PaymentResponse payment, boolean created) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean hasIdempotencyKey(String key) {
        return payments.findByIdempotencyKey(key).isPresent();
    }

    public Reservation reserve(UUID orderId, String key, BigDecimal amount) {
        var order = orders.findLockedById(orderId).orElseThrow(() ->
                new PaymentException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found."));
        var existing = payments.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            var payment = existing.get();
            if (!payment.getOrderId().equals(orderId) || payment.getAmount().compareTo(amount) != 0) {
                throw new PaymentException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                        "This key is already associated with a different request.");
            }
            return new Reservation(PaymentResponse.from(payment, orderId), false);
        }
        if (order.getStatus() != OrderStatus.CREATED || payments.existsByOrder_Id(orderId)) {
            throw new PaymentException(HttpStatus.CONFLICT, "ORDER_NOT_PAYABLE",
                    "This order is paid or has an existing payment attempt requiring review.");
        }
        if (order.getTotalAmount() == null || amount.compareTo(order.getTotalAmount()) != 0) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "AMOUNT_MISMATCH",
                    "Payment amount must equal the order total.");
        }
        var payment = payments.saveAndFlush(new PaymentEntity(order, amount, key));
        return new Reservation(PaymentResponse.from(payment, orderId), true);
    }

    public PaymentResponse complete(UUID orderId, UUID paymentId, String providerPaymentId) {
        var order = orders.findLockedById(orderId).orElseThrow();
        var payment = payments.findById(paymentId).orElseThrow();
        if (payment.getStatus() != PaymentStatus.PENDING || !payment.getOrderId().equals(orderId)) {
            throw new IllegalStateException("Payment is not pending for this order");
        }
        payment.authorize(providerPaymentId);
        order.markPaid();
        return PaymentResponse.from(payment, orderId);
    }

    public void markUnknown(UUID orderId, UUID paymentId) {
        orders.findLockedById(orderId).orElseThrow();
        var payment = payments.findById(paymentId).orElseThrow();
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.markUnknown();
        }
    }
}
