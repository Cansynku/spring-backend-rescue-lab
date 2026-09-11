package dev.javiercano.backendrescue.order;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderRepository orders;

    public OrderService(OrderRepository orders) { this.orders = orders; }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        return OrderResponse.from(orders.save(new PurchaseOrderEntity(request.customerEmail(), request.totalAmount())));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return OrderResponse.from(orders.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list() {
        return orders.findSummaries();
    }

    @Transactional(readOnly = true)
    public OrderPage page(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        }
        var result = orders.findSummaryPage(org.springframework.data.domain.PageRequest.of(page, size));
        return new OrderPage(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages());
    }
}
