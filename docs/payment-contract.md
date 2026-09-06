# Payment reliability increment — contract decision

Scope: BR-001, BR-002 and BR-003, with the payment input/error handling required to make them usable. This increment does not claim to close the other seven findings.

## Client contract (breaking relative to baseline-v1)

- `POST /api/orders/{id}/payments` requires `Idempotency-Key`: 1–128 ASCII letters, digits, `.`, `_` or `-`.
- A payment must be positive, have at most two decimal places, and equal the order total. There are no partial payments or currency conversion in this lab.
- The key identifies one order and one numeric amount. Equivalent decimal representations (25.5 and 25.50) are the same request. Reusing a key for a different request is 409.
- A new successful payment returns 201. A completed replay returns 200 with the original payment ID. A request already in progress returns 202 with its payment ID and `PENDING` status. Clients must understand this additional enum value.
- A provider error, timeout, invalid provider result, or failed local completion is treated conservatively as an uncertain outcome. The API returns 503 with an application error code. Retries of that key never submit another charge. Another key cannot bypass an unresolved attempt or pay an already paid order.
- Missing order: 404. Missing key or invalid amount/key: 400. An order that is not payable or a changed request: 409.

Known consumers: the repository's happy-path test and PowerShell smoke script; both will be updated. No external consumer deployment is known or asserted.

## Durable workflow

1. Short transaction: lock the order, validate key/request and order state, create a durable `PENDING` payment with a database-unique idempotency key, then commit.
2. Call the provider outside any database transaction, once, with explicit connect/read timeouts.
3. Short transaction: authorize that payment and mark the order PAID atomically; or persist `UNKNOWN` when the provider outcome cannot be established.

Concurrent requests for the same order serialize only while reserving/completing database state. The provider call is outside that lock. Database uniqueness prevents a key being claimed by different orders concurrently.

## Failure and migration limits

- A process crash after reservation can leave `PENDING` indefinitely. A finalization failure can leave `PENDING` or `UNKNOWN`. These block additional charges until reconciliation. This increment prioritizes avoiding duplicate submissions over automatic recovery.
- There is no distributed transaction or exactly-once claim. The local provider is a simulator with no reconciliation API. Recovery/reconciliation requires a future explicit provider contract; do not delete an uncertain attempt or retry with a new key as a workaround.
- New schema: nullable unique idempotency key (legacy rows have none), plus PENDING and UNKNOWN enum values. Existing database constraints require a versioned migration; `ddl-auto=update` is not an upgrade guarantee. This increment is validated against a **fresh isolated database**. The original baseline database is preserved. Migration is part of BR-009.
- Authentication, owner-scoped keys and a production security model remain BR-008. This is a localhost educational API only.
