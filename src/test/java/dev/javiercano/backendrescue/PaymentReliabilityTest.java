package dev.javiercano.backendrescue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.javiercano.backendrescue.order.OrderRepository;
import dev.javiercano.backendrescue.payment.PaymentRepository;
import dev.javiercano.backendrescue.payment.PaymentTransactions;
import dev.javiercano.backendrescue.payment.PaymentStatus;
import dev.javiercano.backendrescue.order.OrderStatus;
import dev.javiercano.backendrescue.provider.PaymentProviderClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
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
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentReliabilityTest {
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final AtomicInteger RESPONSE_STATUS = new AtomicInteger(200);
    private static final AtomicInteger DELAY_MS = new AtomicInteger();
    private static final AtomicReference<String> RESPONSE_BODY = new AtomicReference<>();
    private static final ExecutorService HTTP_THREADS = Executors.newCachedThreadPool();
    private static final HttpServer PROVIDER = startProvider();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @MockitoSpyBean PaymentProviderClient providerClient;
    @MockitoSpyBean PaymentTransactions transactions;

    private static HttpServer startProvider() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(HTTP_THREADS);
            server.createContext("/charges", exchange -> {
                CALLS.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                int responseStatus = RESPONSE_STATUS.get();
                String responseBody = RESPONSE_BODY.get();
                try {
                    Thread.sleep(DELAY_MS.get());
                    var bytes = responseBody.getBytes(StandardCharsets.UTF_8);
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
        RESPONSE_BODY.set("{\"paymentId\":\"sandbox-reliability\"}");
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

    @Test
    void concurrentReplaySeesCommittedPendingIntentWhileProviderIsBlocked() throws Exception {
        var order = order();
        var providerEntered = new CountDownLatch(1);
        var releaseProvider = new CountDownLatch(1);
        doAnswer(invocation -> {
            providerEntered.countDown();
            if (!releaseProvider.await(5, TimeUnit.SECONDS)) { throw new IllegalStateException("Fixture timed out"); }
            return invocation.callRealMethod();
        }).when(providerClient).charge(any(), any());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> pay(order, "in-flight-key", "25.50"));
            try {
                assertThat(providerEntered.await(3, TimeUnit.SECONDS)).isTrue();
                var retry = executor.submit(() -> pay(order, "in-flight-key", "25.50")).get(2, TimeUnit.SECONDS);
                assertThat(retry.getResponse().getStatus()).isEqualTo(202);
                assertThat(body(retry).get("status").asText()).isEqualTo("PENDING");
                assertThat(payments.count()).isEqualTo(1);
                // Another key must not bypass the unresolved attempt.
                assertThat(pay(order, "bypass-key", "25.50").getResponse().getStatus()).isEqualTo(409);
            } finally {
                releaseProvider.countDown();
            }
            assertThat(first.get(3, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(201);
        }
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void simultaneousDifferentOrdersCannotClaimTheSameKey() throws Exception {
        var firstOrder = order();
        var secondOrder = order();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return pay(firstOrder, "race-key", "25.50"); });
            var second = executor.submit(() -> { start.await(); return pay(secondOrder, "race-key", "25.50"); });
            start.countDown();
            assertThat(java.util.List.of(first.get(5, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(5, TimeUnit.SECONDS).getResponse().getStatus())).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(CALLS.get()).isEqualTo(1);
        assertThat(payments.count()).isEqualTo(1);
    }

    @Test
    void providerErrorRemainsUnknownWithoutRetryingOrMarkingOrderPaid() throws Exception {
        var order = order();
        RESPONSE_STATUS.set(503);
        var result = pay(order, "provider-error-key", "25.50");
        assertThat(result.getResponse().getStatus()).isEqualTo(503);
        assertThat(body(result).get("code").asText()).isEqualTo("PAYMENT_OUTCOME_UNKNOWN");
        assertThat(payments.findByIdempotencyKey("provider-error-key").orElseThrow().getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(orders.findById(java.util.UUID.fromString(order)).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(pay(order, "provider-error-key", "25.50").getResponse().getStatus()).isEqualTo(503);
        assertThat(pay(order, "provider-bypass-key", "25.50").getResponse().getStatus()).isEqualTo(409);
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void failedLocalCompletionDoesNotResubmitSuccessfulProviderCharge() throws Exception {
        var order = order();
        doThrow(new DataAccessResourceFailureException("Simulated database finalization failure"))
                .when(transactions).complete(any(), any(), anyString());
        assertThat(pay(order, "completion-key", "25.50").getResponse().getStatus()).isEqualTo(503);
        assertThat(pay(order, "completion-key", "25.50").getResponse().getStatus()).isEqualTo(503);
        assertThat(payments.findByIdempotencyKey("completion-key").orElseThrow().getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(orders.findById(java.util.UUID.fromString(order)).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void completionTransactionRollsBackOrderAndPaymentTogether() throws Exception {
        var order = order();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new DataAccessResourceFailureException("Failure before completion transaction commits");
        }).when(transactions).complete(any(), any(), anyString());
        assertThat(pay(order, "rollback-key", "25.50").getResponse().getStatus()).isEqualTo(503);
        var stored = payments.findByIdempotencyKey("rollback-key").orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(stored.getProviderPaymentId()).isNull();
        assertThat(orders.findById(java.util.UUID.fromString(order)).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void failedUncertaintyWriteLeavesDurablePendingIntentThatBlocksNewCharges() throws Exception {
        var order = order();
        doThrow(new DataAccessResourceFailureException("Simulated database unavailable"))
                .when(transactions).complete(any(), any(), anyString());
        doThrow(new DataAccessResourceFailureException("Simulated database unavailable"))
                .when(transactions).markUnknown(any(), any());
        assertThat(pay(order, "durable-key", "25.50").getResponse().getStatus()).isEqualTo(503);
        assertThat(pay(order, "durable-key", "25.50").getResponse().getStatus()).isEqualTo(202);
        assertThat(pay(order, "new-durable-key", "25.50").getResponse().getStatus()).isEqualTo(409);
        assertThat(payments.findByIdempotencyKey("durable-key").orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"paymentId\":\"\"}", "not-json"})
    void invalidProviderResponseIsAnUncertainOutcome(String response) throws Exception {
        RESPONSE_BODY.set(response);
        assertThat(pay(order(), "invalid-provider-key", "25.50").getResponse().getStatus()).isEqualTo(503);
        assertThat(payments.findByIdempotencyKey("invalid-provider-key").orElseThrow().getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "null", "25.501", "1.00"})
    void rejectsInvalidOrMismatchedAmounts(String amount) throws Exception {
        assertThat(pay(order(), "amount-key", amount).getResponse().getStatus()).isEqualTo(400);
        assertThat(CALLS.get()).isZero();
        assertThat(payments.count()).isZero();
    }

    @Test
    void missingKeyAndMissingOrderHaveDefinedErrors() throws Exception {
        mvc.perform(post("/api/orders/{id}/payments", order()).contentType("application/json")
                .content("{\"amount\":25.50}")).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_REQUEST"));
        assertThat(pay(java.util.UUID.randomUUID().toString(), "missing-order-key", "25.50")
                .getResponse().getStatus()).isEqualTo(404);
        assertThat(CALLS.get()).isZero();
    }

    @Test
    void changedAmountCannotReplayAKey() throws Exception {
        var order = order();
        pay(order, "changed-amount-key", "25.50");
        assertThat(pay(order, "changed-amount-key", "24.50").getResponse().getStatus()).isEqualTo(409);
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "invalid key", "invalid/key"})
    void rejectsInvalidKeys(String key) throws Exception {
        assertThat(pay(order(), key, "25.50").getResponse().getStatus()).isEqualTo(400);
        assertThat(CALLS.get()).isZero();
    }
}
