package dev.javiercano.backendrescue.payment;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody CreatePaymentRequest request) {
        var result = service.pay(orderId, key, request);
        return ResponseEntity.status(result.httpStatus()).body(result.payment());
    }
}
