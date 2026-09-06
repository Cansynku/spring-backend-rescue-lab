# Combined review of draft PRs 1–3

Review date: 2026-09-06. Verdict at the reviewed heads: **changes required before accepting the combined increment**. Green CI verifies existing cases; it does not resolve the cross-endpoint contract and logging gaps below. The findings preserve that historical snapshot; see the REV-01 follow-up below for its correction. This review does not merge, approve or deploy a PR.

## Immutable scope and evidence

| PR | Reviewed head | Base | Current remote state |
| --- | --- | --- | --- |
| 1, payment reliability | `64ea86358a02a09f4953ddab46642279988fe29f` | baseline | Draft, not merged; H2/PostgreSQL checks successful |
| 2, schema migrations | `c6292380995407fb2b43bdef968fb4c3684e95de` | rescue/payment-reliability | Draft, not merged; H2/PostgreSQL checks successful |
| 3, order validation/query | `e6b9987b563b408bff62bb9a9349dff5c7874a7c` | rescue/schema-migrations | Draft, not merged; H2/PostgreSQL checks successful |

The combined change is compared with `baseline-v1` (`8ace755019969500a22b17a855a00e00d24711dd`). Reviewed the payment transaction boundaries, provider failure handling, DTOs/errors, migration/adoption guard, query mapping, regression tests and delivery configuration. This is a targeted backend review, not an exhaustive security audit.

Previous full-build evidence at PR 3: 51 PostgreSQL tests passed; H2 47 passed and 4 PostgreSQL-only migration cases skipped. Remote head/check state was refreshed in this review. Two additional HTTP probes ran against the same source in the ignored validation checkout on H2 and confirmed findings R1/R3. Those probes assert current defective behavior and are not permanent regression coverage or fixes. Existing PostgreSQL log evidence confirms R2. No original application database was targeted.

## Findings

### R1 — P2: accepted orders can exceed the payment contract

`order/CreateOrderRequest.java:8` permits 36 integer digits, while `payment/CreatePaymentRequest.java:8` permits only 17. Payment must equal the entire order total; partial payments are not available. An order with total `100000000000000000.00` returns 201, but payment of exactly that total returns 400 `INVALID_PAYMENT_REQUEST` before a payment is persisted. This was reproduced through MockMvc against the combined head, not merely inferred from annotations.

The mismatch originates in the payment restriction and remains explicit in PR 3's new order contract and maximum-value test. It should not be described as a newly introduced storage problem. Database capacity alone is not a valid payable-order business limit.

Next change: align new order validation with the existing 17-digit payment limit and document it, preserving legacy reads and storage. This conservative proposal avoids widening the payment contract. If 36-digit payments are actually required, choose that domain contract explicitly instead. Add an end-to-end create/pay test at the shared maximum, and verify the next value is rejected before order creation. Update the current 36-digit acceptance test. Existing oversized legacy orders need an explicit unsupported-payment explanation; do not rewrite their amounts.

### R2 — P2: framework logging reveals the idempotency key on a conflict

The application-owned messages in `PaymentService` avoid raw keys, but the default `org.hibernate.engine.jdbc.spi.SqlExceptionHelper` logger emits PostgreSQL's unique-violation detail before the exception is translated into 409. The previous full PostgreSQL run, `.local/orders-postgres.log:193-195`, contains a duplicate-constraint message followed by `Key (idempotency_key)=(<REDACTED>) already exists.` The fixture value is synthetic; no real credential exposure is asserted.

The reproducible trigger is `PaymentReliabilityTest.simultaneousDifferentOrdersCannotClaimTheSameKey`. A policy about safe application messages does not cover this framework path. Idempotency keys are client-controlled data and must not appear in diagnostic logs under the proposed redaction policy.

Next change: prevent raw SQL exception details from entering application logs and retain a safe structured event with error category, SQL state/known constraint classification where appropriate, and a server-generated correlation ID. Validate the actual PostgreSQL driver/Hibernate behavior before selecting the narrow configuration or interception mechanism. Do not suppress all operational errors to hide the leak. Capture logs during the real unique-constraint race and assert that a synthetic key, email and sensitive provider-body marker are absent while the safe event remains present.

### R3 — P2: malformed payment identifiers bypass the error contract

`payment/PaymentErrorHandler.java:20-21` handles validation, missing headers and unreadable JSON but omits `MethodArgumentTypeMismatchException`. `POST /api/orders/not-a-uuid/payments` with a valid body/key returns 400 without `INVALID_PAYMENT_REQUEST`, whereas the corresponding order handler defines its application code. The isolated MockMvc probe confirmed the missing code. Its precise production error-dispatch body has not been checked with a packaged server.

Next change: handle malformed payment identifiers within the payment advice; keep 400, return ProblemDetail and `INVALID_PAYMENT_REQUEST`, and do not echo the raw identifier. Test both endpoint families for malformed identifiers, missing resources, malformed JSON and invalid input, including content type and absence of submitted values.

## Ordered follow-up and acceptance criteria

| Work item | Scope | Acceptance evidence |
| --- | --- | --- |
| REV-01 | Align payable order/payment limits (R1) | Shared maximum creates and pays once; next value rejects before persistence; existing nullable/oversized rows remain readable |
| REV-02 | Complete expected client-error shapes (R3) | Stable 400/404/409 codes and ProblemDetail across order/payment endpoints; invalid requests make no provider calls; do not turn unexpected storage failures into a claimed key conflict |
| REV-03 | Redact framework failure paths (R2) | Real PostgreSQL collision logs contain safe diagnostics but no synthetic sensitive markers; safe handling of provider and persistence failures remains observable |
| REV-04 | Add request correlation and operational events | Server-generated ID in response and safe events; separate IDs on parallel requests; context cleared between requests; replay, conflict, uncertainty and persistence failure are distinguishable without raw bodies/keys |

REV-01 through REV-03 address this review's findings. REV-04 is a scoped BR-010 improvement, not a defect already covered by the existing tests. The event proposal is limited to a generated request ID, event name, application-owned payment ID when available, result/status and bounded failure category. Avoid raw URLs, query strings, client-supplied IDs, email, idempotency keys, provider bodies and exception messages. Publish a field allowlist and test redaction and context cleanup; do not assume MDC alone proves either.

For expected errors, preserve existing `ORDER_NOT_FOUND`, `INVALID_ORDER_REQUEST`, `INVALID_PAYMENT_REQUEST`, `INVALID_IDEMPOTENCY_KEY`, `AMOUNT_MISMATCH`, `IDEMPOTENCY_CONFLICT`, `ORDER_NOT_PAYABLE` and `PAYMENT_OUTCOME_UNKNOWN`. Any new unexpected-failure code needs an explicit mapping and tests; the current broad `DataIntegrityViolationException` to key-conflict translation should be narrowed to the verified uniqueness case as part of REV-02. This latter concern is confirmed by code inspection, not a newly reproduced non-unique constraint failure.

## Preserved boundaries and remaining uncertainty

- Payment reservation commits before HTTP; completion uses a short independent transaction. Existing tests protect replay, concurrency, uncertainty and rollback. No exactly-once or automatic recovery claim is justified.
- Flyway migration cases protect the two known legacy shapes and preserved values. The packaged-app adoption check on the local baseline copy remains pending: automatic approval review previously rejected that startup without a reason. It was not retried here. Exclusive maintenance access remains required because guard and adoption use separate connections.
- The grouped order projection has bounded statement count; the list remains unpaginated. No latency/load benchmark is claimed.
- Authentication/ownership, provider reconciliation and crash recovery still require domain decisions and separate work. Do not expose this simulator as a production service.
- Resolve and review changes before considering integration. The dependency order is PR 1, then PR 2, then PR 3; confirm each base/diff after parent integration. Do not merge a child into an obsolete parent branch as a substitute for integration into the intended delivery branch. No merge authorization is implied by this report.

The review itself did not change application source. Subsequent REV-01/REV-02 corrections are described below; R2 (REV-03) and REV-04 remain pending.

## REV-01 follow-up

Order creation now uses the existing payment limit of 17 integer digits and two fractional digits. No schema or payment limit changed. The boundary regression first failed against `e6b9987` (expected 400, observed 201). Added coverage creates and pays the maximum, replays it with one provider call, verifies exact persisted amounts, rejects the next cent before persistence/provider access, and reads an oversized nullable legacy order without rewriting it. The former 36-digit acceptance test now uses the payable maximum. See the current PR checks for final validation. REV-02, REV-03 and REV-04 are not implemented by this correction.

## REV-02 follow-up

Malformed payment UUIDs now receive 400 `INVALID_PAYMENT_REQUEST` ProblemDetail. Invalid order/payment requests use a fixed URN instance, avoiding Spring's automatic reflection of a malformed path. Existing 400/404/409 codes are retained. Tests check content type, status/code and absence of synthetic private input/SQL detail in complete response bodies.

Reservation failures no longer all claim a key conflict. SQLSTATE 23505 plus a matching persisted key, read after rollback in a new transaction, establishes the collision without relying on legacy constraint names. Other integrity failures return a safe 500 `PAYMENT_PERSISTENCE_FAILED`; data-access failures during reservation or key verification return 503 `PAYMENT_STORAGE_UNAVAILABLE`. The provider is not called on those paths. A safe category event records the storage failure without its raw message. This does not solve the separate Hibernate log detail issue (REV-03).

Tests cover malformed UUID/JSON, missing orders, changed requests, injected not-null/foreign-key/other-unique failures, unavailable reservation storage and failed key verification. Existing H2/PostgreSQL concurrency tests continue to protect real unique-key races and single provider submission. Injection establishes the error translation behavior, not a real database-outage experiment. Framework 405/415/unknown-route handling and non-data-access infrastructure exceptions are outside this increment's expected-error contract.

Final local clean builds: PostgreSQL 61 tests passed, no skips; H2 57 passed with 4 intentional PostgreSQL-only skips. The PostgreSQL run also recorded an actual SQLSTATE 23505 collision in the concurrency test. The remaining raw framework detail in that log is still REV-03's reproduced problem, not resolved by safe HTTP error messages.
