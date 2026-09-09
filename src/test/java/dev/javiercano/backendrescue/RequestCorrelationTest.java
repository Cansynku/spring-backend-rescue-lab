package dev.javiercano.backendrescue;

import dev.javiercano.backendrescue.config.RequestCorrelationFilter;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;

class RequestCorrelationTest {
    @Test
    void ignoresUntrustedHeaderAndClearsContextAfterFailure() {
        var request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader("X-Request-ID", "untrusted-input");
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> new RequestCorrelationFilter().doFilter(request, response, (req, res) -> {
            assertThat(MDC.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
            throw new jakarta.servlet.ServletException("test failure");
        })).isInstanceOf(jakarta.servlet.ServletException.class);
        assertThat(UUID.fromString(response.getHeader("X-Request-ID"))).isNotNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void concurrentRequestsHaveDistinctContextsAndReusedThreadsAreClean() throws Exception {
        var filter = new RequestCorrelationFilter();
        var rendezvous = new CyclicBarrier(2);
        Callable<String> call = () -> {
            assertThat(MDC.get("requestId")).isNull();
            var response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", "/api/orders"), response, (req, res) -> {
                var id = MDC.get("requestId");
                try { rendezvous.await(5, TimeUnit.SECONDS); }
                catch (Exception error) { throw new jakarta.servlet.ServletException(error); }
                assertThat(MDC.get("requestId")).isEqualTo(id).isEqualTo(response.getHeader("X-Request-ID"));
            });
            assertThat(MDC.get("requestId")).isNull();
            return response.getHeader("X-Request-ID");
        };
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(call);
            var second = workers.submit(call);
            assertThat(first.get(8, TimeUnit.SECONDS)).isNotEqualTo(second.get(8, TimeUnit.SECONDS));
            var third = workers.submit(call);
            var fourth = workers.submit(call);
            assertThat(third.get(8, TimeUnit.SECONDS)).isNotEqualTo(fourth.get(8, TimeUnit.SECONDS));
        }
    }
}
