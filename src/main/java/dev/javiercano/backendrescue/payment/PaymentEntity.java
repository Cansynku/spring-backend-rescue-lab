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
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    protected PaymentEntity() {}

    public PaymentEntity(PurchaseOrderEntity order, BigDecimal amount, String providerPaymentId, PaymentStatus status) {
        this.order = order;
        this.amount = amount;
        this.providerPaymentId = providerPaymentId;
        this.status = status;
    }

    public UUID getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public PaymentStatus getStatus() { return status; }
}
