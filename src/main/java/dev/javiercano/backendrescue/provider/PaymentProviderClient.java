package dev.javiercano.backendrescue.provider;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentProviderClient {
    private final RestClient client;

    public PaymentProviderClient(@Value("${payment-provider.base-url}") String baseUrl,
            @Value("${payment-provider.connect-timeout-ms:1000}") int connectTimeoutMs,
            @Value("${payment-provider.read-timeout-ms:2000}") int readTimeoutMs) {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("Provider timeouts must be positive");
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl)
                .requestFactory(factory).build();
    }

    public ProviderChargeResponse charge(UUID orderId, BigDecimal amount) {
        return client.post().uri("/charges").body(new ProviderChargeRequest(orderId, amount))
                .retrieve().body(ProviderChargeResponse.class);
    }
}
