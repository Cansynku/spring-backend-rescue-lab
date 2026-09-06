package dev.javiercano.backendrescue.provider;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderChargeRequest(UUID orderId, BigDecimal amount) {}
