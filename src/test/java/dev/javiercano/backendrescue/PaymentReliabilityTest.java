package dev.javiercano.backendrescue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.javiercano.backendrescue.order.OrderRepository;
import dev.javiercano.backendrescue.payment.PaymentRepository;
import dev.javiercano.backendrescue.provider.PaymentProviderClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentReliabilityTest {
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final AtomicInteger RESPONSE_STATUS = new AtomicInteger(200);
    private static final AtomicInteger DELAY_MS = new AtomicInteger();
    private static final ExecutorService HTTP_THREADS = Executors.newCachedThreadPool();
    private static final HttpServer PROVIDER = startProvider();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @MockitoSpyBean PaymentProviderClient providerClient;

    private static HttpServer startProvider() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(HTTP_THREADS);
            server.createContext("/charges", exchange -> {
                CALLS.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                int responseStatus = RESPONSE_STATUS.get();
                try {
                    Thread.sleep(DELAY_MS.get());
                    var bytes = "{\"paymentId\":\"sandbox-reliability\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(responseStatus, bytes.length);
                    exchange.getResponseBody().write(bytes);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (IOException ignored) {
                    // A timed-out client may close before the fixture writes its response.
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("payment-provider.base-url", () -> "http://127.0.0.1:" + PROVIDER.getAddress().getPort());
        registry.add("payment-provider.connect-timeout-ms", () -> 200);
        registry.add("payment-provider.read-timeout-ms", () -> 200);
    }

    @BeforeEach
    void reset() {
        payments.deleteAll();
        orders.deleteAll();
        CALLS.set(0);
        RESPONSE_STATUS.set(200);
        DELAY_MS.set(0);
    }

    @AfterAll
    static void stop() {
        PROVIDER.stop(0);
        HTTP_THREADS.shutdownNow();
    }

    private String order() throws Exception {
        var result = mvc.perform(post("/api/orders").contentType("application/json")
                .content("{\"customerEmail\":\"demo@example.com\",\"totalAmount\":25.50}"))
                .andExpect(status().isCreated()).andReturn();
        return body(result).get("id").asText();
    }

    private MvcResult pay(String order, String key, String amount) throws Exception {
        return mvc.perform(post("/api/orders/{id}/payments", order)
                .header("Idempotency-Key", key).contentType("application/json")
                .content("{\"amount\":" + amount + "}")).andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void repeatedKeyReturnsOriginalPaymentWithoutAnotherCharge() throws Exception {
        var order = order();
        var first = pay(order, "repeat-key", "25.50");
        var replay = pay(order, "repeat-key", "25.5");
        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        assertThat(replay.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(replay).get("id")).isEqualTo(body(first).get("id"));
        assertThat(CALLS.get()).isEqualTo(1);
        assertThat(payments.count()).isEqualTo(1);
    }

    @Test
    void keyCannotBeReusedForAnotherOrder() throws Exception {
        pay(order(), "shared-key", "25.50");
        var second = pay(order(), "shared-key", "25.50");
        assertThat(second.getResponse().getStatus()).isEqualTo(409);
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void paidOrderRejectsAnotherKey() throws Exception {
        var order = order();
        pay(order, "first-key", "25.50");
        assertThat(pay(order, "second-key", "25.50").getResponse().getStatus()).isEqualTo(409);
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void providerRunsOutsideDatabaseTransaction() throws Exception {
        var active = new AtomicBoolean();
        doAnswer(invocation -> {
            active.set(TransactionSynchronizationManager.isActualTransactionActive());
            return invocation.callRealMethod();
        }).when(providerClient).charge(any(), any());
        assertThat(pay(order(), "transaction-key", "25.50").getResponse().getStatus()).isEqualTo(201);
        assertThat(active.get()).as("HTTP provider must run outside a DB transaction").isFalse();
    }

    @Test
    void slowProviderIsBoundedAndDoesNotGetRetried() throws Exception {
        var order = order();
        DELAY_MS.set(1500);
        long start = System.nanoTime();
        var first = pay(order, "timeout-key", "25.50");
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertThat(first.getResponse().getStatus()).isEqualTo(503);
        assertThat(elapsedMs).isLessThan(1200);
        assertThat(pay(order, "timeout-key", "25.50").getResponse().getStatus()).isEqualTo(503);
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void invalidPaymentAmountIsRejectedBeforeCallingProvider() throws Exception {
        var result = pay(order(), "invalid-key", "-1.00");
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(CALLS.get()).isZero();
        assertThat(payments.count()).isZero();
    }
}
