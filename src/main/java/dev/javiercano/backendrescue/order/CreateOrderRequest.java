package dev.javiercano.backendrescue.order;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record CreateOrderRequest(
        @NotBlank @Email @Size(max = 255) String customerEmail,
        @NotNull @Positive @Digits(integer = 36, fraction = 2) BigDecimal totalAmount) {}
