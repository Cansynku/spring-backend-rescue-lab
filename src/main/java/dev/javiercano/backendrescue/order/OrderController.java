package dev.javiercano.backendrescue.order;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/orders/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping
    public List<OrderResponse> list() { return service.list(); }

    @GetMapping("/page")
    public OrderPage page(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return service.page(page, size);
    }
}
