package dev.javiercano.backendrescue;

import dev.javiercano.backendrescue.order.*;
import dev.javiercano.backendrescue.payment.*;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderReliabilityTest {
    @Autowired MockMvc mvc;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired OrderService service;
    @Autowired EntityManagerFactory emf;

    @BeforeEach
    void clean() {
        payments.deleteAll();
        orders.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"customerEmail\":\"\",\"totalAmount\":1}",
            "{\"customerEmail\":\"   \",\"totalAmount\":1}",
            "{\"customerEmail\":\"invalid\",\"totalAmount\":1}",
            "{\"customerEmail\":\"demo@example.com\"}",
            "{\"customerEmail\":\"demo@example.com\",\"totalAmount\":0}",
            "{\"customerEmail\":\"demo@example.com\",\"totalAmount\":-1}",
            "{\"customerEmail\":\"demo@example.com\",\"totalAmount\":1.001}",
            "{\"customerEmail\":\"demo@example.com\",\"totalAmount\":1e36}", "null", "{"})
    void rejectsInvalidInputWithoutPersisting(String body) throws Exception {
        mvc.perform(post("/api/orders").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_REQUEST"));
        assertThat(orders.count()).isZero();
    }

    @Test
    void rejectsEmailLongerThanStorage() throws Exception {
        var email = "a".repeat(245) + "@example.com";
        mvc.perform(post("/api/orders").contentType("application/json")
                        .content("{\"customerEmail\":\"" + email + "\",\"totalAmount\":1}"))
                .andExpect(status().isBadRequest());
        assertThat(orders.count()).isZero();
    }

    @Test
    void acceptsSmallestAndLargestPayablePositiveAmounts() throws Exception {
        for (var amount : new String[]{"0.01", "99999999999999999.99"}) {
            mvc.perform(post("/api/orders").contentType("application/json")
                            .content("{\"customerEmail\":\"demo@example.com\",\"totalAmount\":" + amount + "}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.paymentCount").value(0));
        }
        assertThat(orders.count()).isEqualTo(2);
    }

    @Test
    void oversizedLegacyOrderRemainsReadableWithoutChangingItsAmount() throws Exception {
        var amount = new BigDecimal("100000000000000000.00");
        var legacy = orders.save(new PurchaseOrderEntity(null, amount));
        mvc.perform(get("/api/orders/{id}", legacy.getId())).andExpect(status().isOk());
        assertThat(service.get(legacy.getId()).totalAmount()).isEqualByComparingTo(amount);
        assertThat(service.list()).singleElement().satisfies(order -> {
            assertThat(order.id()).isEqualTo(legacy.getId());
            assertThat(order.totalAmount()).isEqualByComparingTo(amount);
            assertThat(order.customerEmail()).isNull();
        });
        assertThat(orders.findById(legacy.getId()).orElseThrow().getTotalAmount()).isEqualByComparingTo(amount);
    }

    @Test
    void missingAndMalformedIdsHaveStableErrors() throws Exception {
        mvc.perform(get("/api/orders/{id}", UUID.randomUUID())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        var invalid = mvc.perform(get("/api/orders/not-a-uuid")).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_REQUEST")).andReturn();
        assertThat(invalid.getResponse().getContentAsString()).doesNotContain("not-a-uuid");
    }

    @Test
    void listingKeepsCardinalityAndCountsWithOneStatementAsOrdersGrow() {
        var empty = orders.save(new PurchaseOrderEntity("demo@example.com", BigDecimal.ONE));
        assertSingleQuery(Map.of(empty.getId(), 0));
        var multi = orders.save(new PurchaseOrderEntity("demo@example.com", BigDecimal.TEN));
        payments.save(new PaymentEntity(multi, BigDecimal.TEN, "first"));
        var unknown = new PaymentEntity(multi, BigDecimal.TEN, "second");
        unknown.markUnknown();
        payments.save(unknown);
        var legacy = orders.save(new PurchaseOrderEntity(null, null));
        for (int i = 0; i < 8; i++) {
            orders.save(new PurchaseOrderEntity("demo@example.com", BigDecimal.ONE));
        }
        var statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = service.list();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(result).hasSize(11);
        var counts = result.stream().collect(Collectors.toMap(OrderResponse::id, OrderResponse::paymentCount));
        assertThat(counts).containsEntry(empty.getId(), 0).containsEntry(multi.getId(), 2).containsEntry(legacy.getId(), 0);
        assertThat(result.stream().filter(o -> o.id().equals(legacy.getId())).findFirst().orElseThrow().customerEmail()).isNull();
    }

    @Test
    void emptyListIsStillAnArray() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    private void assertSingleQuery(Map<UUID, Integer> expected) {
        var statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = service.list();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(result.stream().collect(Collectors.toMap(OrderResponse::id, OrderResponse::paymentCount)))
                .isEqualTo(expected);
    }

    @Test
    void pagesPreserveCardinalityCountsAndStableOrder() throws Exception {
        var paid = orders.save(new PurchaseOrderEntity("demo@example.com", BigDecimal.TEN));
        payments.save(new PaymentEntity(paid, BigDecimal.TEN, "one"));
        payments.save(new PaymentEntity(paid, BigDecimal.TEN, "two"));
        for (int i = 0; i < 4; i++) orders.save(new PurchaseOrderEntity(null, null));
        var first = service.page(0, 2);
        assertThat(first.totalElements()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(service.page(0, 2).items()).isEqualTo(first.items());
        var all = java.util.stream.IntStream.range(0, 3)
                .boxed().flatMap(page -> service.page(page, 2).items().stream()).toList();
        assertThat(all).hasSize(5);
        assertThat(all.stream().map(OrderResponse::id)).doesNotHaveDuplicates();
        assertThat(all.stream().filter(o -> o.id().equals(paid.getId())).findFirst().orElseThrow().paymentCount()).isEqualTo(2);
        assertThat(service.page(3, 2).items()).isEmpty();
        mvc.perform(get("/api/orders/page").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=no", "page=2147483647&size=100"})
    void invalidPaginationIsRejected(String query) throws Exception {
        mvc.perform(get("/api/orders/page?" + query)).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void emptyPageHasExplicitMetadata() throws Exception {
        mvc.perform(get("/api/orders/page")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }
}
