package dev.javiercano.backendrescue.order;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<PurchaseOrderEntity, UUID> {
    @Query("""
            select new dev.javiercano.backendrescue.order.OrderResponse(
                o.id, o.customerEmail, o.totalAmount, o.status, count(p))
            from PurchaseOrderEntity o left join o.payments p
            group by o.id, o.customerEmail, o.totalAmount, o.status
            """)
    java.util.List<OrderResponse> findSummaries();

    @Query(value = """
            select new dev.javiercano.backendrescue.order.OrderResponse(
                o.id, o.customerEmail, o.totalAmount, o.status, count(p))
            from PurchaseOrderEntity o left join o.payments p
            group by o.id, o.customerEmail, o.totalAmount, o.status
            order by o.id
            """, countQuery = "select count(o) from PurchaseOrderEntity o")
    org.springframework.data.domain.Page<OrderResponse> findSummaryPage(
            org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrderEntity o where o.id = :id")
    Optional<PurchaseOrderEntity> findLockedById(@Param("id") UUID id);
}
