package dev.javiercano.backendrescue.payment;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(@NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {}
