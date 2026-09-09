package dev.javiercano.backendrescue.payment;

import dev.javiercano.backendrescue.order.PurchaseOrderEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PurchaseOrderEntity order;
    private BigDecimal amount;
    private String providerPaymentId;
    @Column(unique = true, length = 128)
    private String idempotencyKey;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    protected PaymentEntity() {}

    public PaymentEntity(PurchaseOrderEntity order, BigDecimal amount, String idempotencyKey) {
        this.order = order;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
    }

    public UUID getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public PaymentStatus getStatus() { return status; }
    public UUID getOrderId() { return order.getId(); }
    public void authorize(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
        this.status = PaymentStatus.AUTHORIZED;
    }
    public void markUnknown() { this.status = PaymentStatus.UNKNOWN; }
}
