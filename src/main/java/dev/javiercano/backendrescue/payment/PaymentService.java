package dev.javiercano.backendrescue.payment;

import dev.javiercano.backendrescue.provider.PaymentProviderClient;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentTransactions transactions;
    private final PaymentProviderClient provider;

    public PaymentService(PaymentTransactions transactions, PaymentProviderClient provider) {
        this.transactions = transactions;
        this.provider = provider;
    }

    public record Result(PaymentResponse payment, HttpStatus httpStatus) {}

    // Reject any caller transaction so the HTTP request never inherits one.
    @Transactional(propagation = Propagation.NEVER)
    public Result pay(UUID orderId, String key, CreatePaymentRequest request) {
        if (key == null || !key.matches("[A-Za-z0-9._-]{1,128}")) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key must contain 1 to 128 letters, digits, dots, underscores or hyphens.");
        }
        PaymentTransactions.Reservation reservation;
        try {
            reservation = transactions.reserve(orderId, key, request.amount());
        } catch (DataIntegrityViolationException conflict) {
            throw new PaymentException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                    "This key could not be reserved. Reuse the original request when checking its outcome.");
        }
        var payment = reservation.payment();
        if (!reservation.created()) {
            return switch (payment.status()) {
                case AUTHORIZED -> new Result(payment, HttpStatus.OK);
                case PENDING -> new Result(payment, HttpStatus.ACCEPTED);
                default -> throw unknownOutcome();
            };
        }
        try {
            var charge = provider.charge(orderId, request.amount());
            if (charge == null || charge.paymentId() == null || charge.paymentId().isBlank()
                    || charge.paymentId().length() > 255) {
                throw new IllegalStateException("Provider returned an unusable payment identifier");
            }
            var completed = transactions.complete(orderId, payment.id(), charge.paymentId());
            LOG.info("payment_authorized paymentId={}", payment.id());
            return new Result(completed, HttpStatus.CREATED);
        } catch (RuntimeException failure) {
            // HTTP failure does not prove the provider did not charge. Never retry it here.
            LOG.warn("payment_outcome_unknown paymentId={} failureType={}", payment.id(), failure.getClass().getSimpleName());
            try {
                transactions.markUnknown(orderId, payment.id());
            } catch (RuntimeException persistenceFailure) {
                LOG.error("payment_reconciliation_required paymentId={} failureType={}",
                        payment.id(), persistenceFailure.getClass().getSimpleName());
            }
            throw unknownOutcome();
        }
    }

    private PaymentException unknownOutcome() {
        return new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_OUTCOME_UNKNOWN",
                "Payment outcome is uncertain. Reconciliation is required; do not submit another payment.");
    }
}
