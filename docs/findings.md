# Intentional finding inventory — baseline

These are seeded educational limitations, not findings from a client engagement. Severity describes the **hypothetical production impact**; this local simulator never charges money. Exactly ten findings define the Rescue scope. Code observations below are distinct from tested exploit/failure reproductions.

| ID | Initial severity | Code evidence | Impact and planned verification |
| --- | --- | --- | --- |
| BR-001 | High | `PaymentService.pay` always calls the provider and inserts a payment; no idempotency key or uniqueness constraint | Retrying may duplicate charges. Reproduce repeated and concurrent requests; fix with an explicit idempotency contract and persistence guarantee. |
| BR-002 | High | `@Transactional` covers lookup, HTTP request and persistence in `PaymentService.pay` | Slow provider holds a DB transaction; provider success followed by DB failure can diverge. Reproduce delay/failure, then design transaction boundaries and recovery; merely moving the HTTP call is insufficient. |
| BR-003 | High | `PaymentProviderClient` uses `SimpleClientHttpRequestFactory` without explicit timeouts | The application has no owned upper bound on provider latency. Reproduce delayed responses; add bounded connect/read policy. |
| BR-004 | Medium | Request records have no constraints; controllers lack `@Valid`; payment amount is not checked against order total | Invalid/negative/mismatched values may be persisted or treated as paid. Reproduce null, malformed email and amount cases; add agreed rules. |
| BR-005 | Medium | `OrderService.list` loads orders, then `OrderResponse.from` calls lazy `payments.size()` | Query count can grow with order count. Measure SQL count for multiple orders, then fix fetch/query strategy without breaking response cardinality. |
| BR-006 | Medium | Missing order uses `ResponseStatusException(404)` in `OrderService`, but `IllegalArgumentException` in `PaymentService` | Equivalent missing resources yield inconsistent errors. Reproduce through HTTP and establish a consistent error contract. |
| BR-007 | Medium | `BaselineHappyPathTest` contains only three successful scenarios on H2 | Failure/concurrency and PostgreSQL-specific behavior are not protected by automated regression tests. Add meaningful failure tests and PostgreSQL Testcontainers during Rescue. |
| BR-008 | High | `SecurityConfiguration` disables CSRF and permits every request | No authentication/ownership policy protects order data if exposed. Define API clients and access model before fixing; test unauthorized/cross-owner calls. |
| BR-009 | Medium | `application.yml` sets `ddl-auto: update` | Schema changes lack versioned/reproducible migration history. Add Flyway and test empty/existing schema upgrades. |
| BR-010 | Low/Medium | No application payment/correlation logging; only framework logs | Operators cannot reliably correlate payment outcome across systems. Add useful, redacted correlation and failure logs; verify no sensitive payload leakage. |

## Rescue record template

For each finding preserve: reproduction command/test, expected versus actual behavior, code location, impact assumptions, smallest fix, checks and result. Do not label an unrun reproduction as verified. Baseline automated tests remain happy-path-only by design.
