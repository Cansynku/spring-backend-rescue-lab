# Backend Rescue — payment reliability increment

Historical evidence for PR #1. The follow-up migration branch adds [BR-009 evidence](schema-migrations.md); migration limitations below describe the first payment increment.

## Executive summary

The `baseline-v1` API accepted repeated payment submissions and performed HTTP during a database transaction. Six regression tests first failed against that implementation. This branch now stores a durable intent, serializes payment reservation for each order, enforces a unique request key, calls the provider outside the transaction, and conservatively retains uncertain outcomes without resubmission.

This is a focused improvement to an educational simulator, not approval to deploy a payment service. Authentication, existing-database migration and payment reconciliation remain open.

## Before → after evidence

Reproduction commit: `15a510058d562e86152afe3124364369ab5f3972`. Command: `mvn -B -ntp -Dtest=PaymentReliabilityTest test`. Observed result against baseline application code: **6 tests, 6 failures, 0 errors, 0 skipped**.

| Scenario | Observed before | Required behavior now verified |
| --- | --- | --- |
| Repeat same key, order and numeric amount | New 201 payment | 200 with original payment ID, one provider call and one payment |
| Reuse a key for another order | Accepted with 201 | 409, no second provider call |
| Pay a paid order using another key | Accepted with 201 | 409, no second provider call |
| Provider call transaction boundary | Transaction active during HTTP | No active transaction during HTTP |
| Provider delayed 1500 ms; configured read timeout 200 ms | Eventually accepted with 201 | 503 within test bound; repeated request does not call provider again |
| Negative amount | Accepted with 201 | 400, no provider call or payment persisted |

Additional tests verify an in-flight replay returning PENDING/202 without waiting for the provider; a different key blocked while the attempt is unresolved; simultaneous cross-order key claims; provider 503/malformed/missing-ID responses; failed finalization; failed uncertainty persistence; and atomic rollback of both order and payment state before the completion transaction commits.

## Validation performed locally

- Java 21, Maven 3.9.11, Spring Boot 3.5.16.
- H2 full build: **27 tests, 0 failures/errors/skipped** before the final rollback test was added.
- PostgreSQL 17.11 full build including that test: **28 tests, 0 failures/errors/skipped**, `BUILD SUCCESS`. The output confirmed PostgreSQL 17.11 as the active database.
- Tests use a dedicated `backend_rescue_test` database. A separate fresh `backend_rescue_payments` database serves the running application; baseline data is preserved.
- Packaged application smoke: order 201, payment 201, replay 200 with the same ID, persisted PAID state, exactly one payment, order found in list.
- Restart persistence was not verified: automatic approval review rejected the additional local stop/restart command. No restart workaround was attempted. Database persistence is tested through fresh transactions and concurrent requests, which is distinct from a process-restart test.
- GitHub CI is configured to run the final suite on H2 and PostgreSQL 17. Remote results belong to the workflow run for the branch's commit; configuration alone is not evidence of success.

## Finding status

| Finding | Status in this increment |
| --- | --- |
| BR-001 | Replay/concurrent resubmission corrected for this API; unique durable key and order reservation verified. No exactly-once guarantee across arbitrary external systems. |
| BR-002 | HTTP removed from database transaction; durable intent and atomic completion/rollback verified. Crash recovery and reconciliation remain open. |
| BR-003 | Explicit positive connect/read timeouts implemented and delayed-read behavior verified. This is not an absolute response deadline against continually streaming peers. |
| BR-004 | Payment validation corrected; order creation validation remains pending. |
| BR-005 | N+1 unchanged; reproduction and fix pending. |
| BR-006 | Payment errors have defined statuses/codes; common errors across the entire API remain pending. |
| BR-007 | Payment failure/concurrency regression coverage added on H2 and PostgreSQL. Testcontainers lifecycle and other feature coverage remain pending. |
| BR-008 | Authentication/authorization unchanged; localhost-only simulator. |
| BR-009 | No versioned migration yet. Use a fresh database; do not upgrade baseline data using ddl-auto. |
| BR-010 | Payment outcome/failure logs use payment ID and exception type, not payloads or provider error bodies. Broader logging/redaction policy remains pending. |

## Review and rollout boundaries

- Breaking request contract: Idempotency-Key is required; new 200/202/409/503 semantics and PENDING/UNKNOWN states. Repository consumers were updated. See [payment contract](payment-contract.md).
- No deployment or merge is included. Keep `baseline-v1` immutable and review this branch separately.
- No employer material, real customer data or payment credentials are used.
- Follow-up: versioned schema migration; order validation/errors and N+1; authentication/ownership contract; explicit reconciliation workflow with a provider capable of reporting charge outcomes.
