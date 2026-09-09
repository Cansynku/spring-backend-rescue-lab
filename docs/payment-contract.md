# Payment reliability increment — contract decision

Scope: BR-001, BR-002 and BR-003, with the payment input/error handling required to make them usable. This increment does not claim to close the other seven findings.

## Client contract (breaking relative to baseline-v1)

- `POST /api/orders/{id}/payments` requires `Idempotency-Key`: 1–128 ASCII letters, digits, `.`, `_` or `-`.
- A payment must be positive, have at most 17 integer digits and two decimal places, and equal the order total. New orders use the same maximum, `99999999999999999.99`. Legacy orders above that amount remain readable but are unsupported by this payment API; their amounts are not rewritten. There are no partial payments or currency conversion in this lab.
- The key identifies one order and one numeric amount. Equivalent decimal representations (25.5 and 25.50) are the same request. Reusing a key for a different request is 409.
- A new successful payment returns 201. A completed replay returns 200 with the original payment ID. A request already in progress returns 202 with its payment ID and `PENDING` status. Clients must understand this additional enum value.
- A provider error, timeout, invalid provider result, or failed local completion is treated conservatively as an uncertain outcome. The API returns 503 with an application error code. Retries of that key never submit another charge. Another key cannot bypass an unresolved attempt or pay an already paid order.
- Missing order: 404. Missing key or invalid amount/key: 400. An order that is not payable or a changed request: 409.
- Malformed payment UUIDs and JSON return 400 ProblemDetail with `INVALID_PAYMENT_REQUEST`. Invalid order requests use `INVALID_ORDER_REQUEST`. Their `instance` is a fixed endpoint-family URN so malformed path input is not echoed; it is not a request correlation ID.
- A reservation uniqueness exception is classified as `IDEMPOTENCY_CONFLICT` only when SQLSTATE 23505 is accompanied by a persisted matching key, checked in a fresh transaction after rollback. This avoids dependence on generated legacy constraint names. Other reservation integrity failures return 500 `PAYMENT_PERSISTENCE_FAILED`. A reservation/key-lookup data-access failure returns 503 `PAYMENT_STORAGE_UNAVAILABLE`. Neither exposes SQL details or calls the provider; use the original key when checking outcomes. Failures after provider submission still follow the existing uncertain-outcome policy.

Known consumers: the repository's happy-path test and PowerShell smoke script; both will be updated. No external consumer deployment is known or asserted.

## Durable workflow

1. Short transaction: lock the order, validate key/request and order state, create a durable `PENDING` payment with a database-unique idempotency key, then commit.
2. Call the provider outside any database transaction, once, with explicit connect/read timeouts.
3. Short transaction: authorize that payment and mark the order PAID atomically; or persist `UNKNOWN` when the provider outcome cannot be established.

Concurrent requests for the same order serialize only while reserving/completing database state. The provider call is outside that lock. Database uniqueness prevents a key being claimed by different orders concurrently.

## Failure and migration limits

- A process crash after reservation can leave `PENDING` indefinitely. A finalization failure can leave `PENDING` or `UNKNOWN`. These block additional charges until reconciliation. This increment prioritizes avoiding duplicate submissions over automatic recovery.
- There is no distributed transaction or exactly-once claim. The local provider is a simulator with no reconciliation API. Recovery/reconciliation requires a future explicit provider contract; do not delete an uncertain attempt or retry with a new key as a workaround.
- New schema: nullable unique idempotency key (legacy rows have none), plus PENDING and UNKNOWN enum values. This branch provides [Flyway migrations and guarded adoption](schema-migrations.md). Existing unmanaged schemas require explicit adoption; there is no automatic schema guessing. The original baseline database is preserved.
- Authentication, owner-scoped keys and a production security model remain BR-008. This is a localhost educational API only.
- The timeout policy bounds connection establishment and blocked reads. It is not a total end-to-end deadline against a peer that continually sends partial data; response size and total-deadline policy remain a hardening consideration.
