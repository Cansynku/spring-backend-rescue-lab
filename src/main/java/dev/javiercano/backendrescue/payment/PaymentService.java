package dev.javiercano.backendrescue.payment;

import dev.javiercano.backendrescue.order.OrderRepository;
import dev.javiercano.backendrescue.provider.PaymentProviderClient;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final PaymentProviderClient provider;

    public PaymentService(OrderRepository orders, PaymentRepository payments, PaymentProviderClient provider) {
        this.orders = orders;
        this.payments = payments;
        this.provider = provider;
    }

    // BR-002: deliberately holds a database transaction during an HTTP call.
    @Transactional
    public PaymentResponse pay(UUID orderId, CreatePaymentRequest request) {
        // BR-006: unlike order lookup, this missing order becomes an unhandled error.
        var order = orders.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        // BR-001: each request creates another simulated charge, including retries.
        var charge = provider.charge(orderId, request.amount());
        var payment = payments.save(new PaymentEntity(order, request.amount(), charge.paymentId(), PaymentStatus.AUTHORIZED));
        order.markPaid();
        // BR-010: no payment outcome/correlation logging in the baseline.
        return PaymentResponse.from(payment, orderId);
    }
}
