package dev.javiercano.backendrescue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DemoHttpTest {
    static final AtomicInteger charges = new AtomicInteger();
    static final HttpServer provider = startProvider();
    @Autowired TestRestTemplate http;

    static HttpServer startProvider() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/charges", exchange -> {
                charges.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                var response = "{\"paymentId\":\"sandbox-ui\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (var out = exchange.getResponseBody()) { out.write(response); }
            });
            server.start(); return server;
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("payment-provider.base-url", () -> "http://127.0.0.1:" + provider.getAddress().getPort());
    }
    @AfterAll static void stop() { provider.stop(0); }

    @Test void servesScreenAndCompletesRealHttpCreatePayReplayFlow() {
        var page = http.getForEntity("/", String.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody()).contains("Nuevo pedido", "/demo.js", "/demo.css");
        assertThat(http.getForEntity("/demo.js", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.getForEntity("/demo.css", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        var created = http.postForEntity("/api/orders", Map.of("customerEmail", "demo@example.com", "totalAmount", "25.50"), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = created.getBody().get("id");
        var headers = new HttpHeaders(); headers.set("Idempotency-Key", UUID.randomUUID().toString());
        var request = new HttpEntity<>(Map.of("amount", "25.50"), headers);
        var paid = http.postForEntity("/api/orders/" + id + "/payments", request, Map.class);
        var replay = http.postForEntity("/api/orders/" + id + "/payments", request, Map.class);
        assertThat(paid.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replay.getBody().get("id")).isEqualTo(paid.getBody().get("id"));
        assertThat(replay.getHeaders().getFirst("X-Request-ID")).isNotBlank()
                .isNotEqualTo(paid.getHeaders().getFirst("X-Request-ID"));
        assertThat(charges.get()).isEqualTo(1);
        var order = http.getForObject("/api/orders/" + id, Map.class);
        assertThat(order.get("status")).isEqualTo("PAID");
        assertThat(order.get("paymentCount")).isEqualTo(1);
    }
}
