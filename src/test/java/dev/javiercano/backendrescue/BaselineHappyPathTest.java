package dev.javiercano.backendrescue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.javiercano.backendrescue.order.OrderRepository;
import dev.javiercano.backendrescue.payment.PaymentRepository;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// BR-007: intentionally covers only successful cases and uses H2.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BaselineHappyPathTest {
    private static final AtomicReference<String> PROVIDER_BODY = new AtomicReference<>();
    private static final HttpServer PROVIDER = startProvider();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;

    private static HttpServer startProvider() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/charges", exchange -> {
                PROVIDER_BODY.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                var response = "{\"paymentId\":\"sandbox-test-charge\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (var output = exchange.getResponseBody()) { output.write(response); }
            });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot start test provider", error);
        }
    }

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        registry.add("payment-provider.base-url", () -> "http://127.0.0.1:" + PROVIDER.getAddress().getPort());
    }

    @BeforeEach
    void cleanDatabase() {
        payments.deleteAll();
        orders.deleteAll();
        PROVIDER_BODY.set(null);
    }

    @AfterAll
    static void stopProvider() { PROVIDER.stop(0); }

    private JsonNode createOrder() throws Exception {
        var response = mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"customerEmail\":\"demo@example.com\",\"totalAmount\":25.50}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.paymentCount").value(0)).andReturn();
        return json.readTree(response.getResponse().getContentAsString());
    }

    @Test
    void createsAndReadsOrder() throws Exception {
        var id = createOrder().get("id").asText();
        mvc.perform(get("/api/orders/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.customerEmail").value("demo@example.com"))
                .andExpect(jsonPath("$.totalAmount").value(25.50));
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void listsOrders() throws Exception {
        createOrder();
        createOrder();
        mvc.perform(get("/api/orders")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void paysThroughHttpProviderAndPersistsResult() throws Exception {
        var id = createOrder().get("id").asText();
        mvc.perform(post("/api/orders/{id}/payments", id).contentType("application/json")
                        .header("Idempotency-Key", "happy-path-payment")
                        .content("{\"amount\":25.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(id))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.providerPaymentId").value("sandbox-test-charge"));
        var sent = json.readTree(PROVIDER_BODY.get());
        assertThat(sent.get("orderId").asText()).isEqualTo(id);
        assertThat(sent.get("amount").decimalValue()).isEqualByComparingTo("25.50");
        assertThat(payments.count()).isEqualTo(1);
        mvc.perform(get("/api/orders/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentCount").value(1));
    }
}
