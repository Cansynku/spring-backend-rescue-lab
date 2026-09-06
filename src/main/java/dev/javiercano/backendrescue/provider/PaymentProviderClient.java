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

    public PaymentProviderClient(@Value("${payment-provider.base-url}") String baseUrl) {
        // BR-003: no explicit connect/read timeouts; relies on system defaults.
        this.client = RestClient.builder().baseUrl(baseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory()).build();
    }

    public ProviderChargeResponse charge(UUID orderId, BigDecimal amount) {
        return client.post().uri("/charges").body(new ProviderChargeRequest(orderId, amount))
                .retrieve().body(ProviderChargeResponse.class);
    }
}
