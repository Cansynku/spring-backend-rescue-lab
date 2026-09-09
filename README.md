# Spring Backend Rescue Lab

An intentionally imperfect Java/Spring Boot Orders & Payments API. The goal is to demonstrate **finding → evidence → impact → fix → verification** using small, reviewable changes.

> **Educational lab — not production-ready.** `baseline-v1` preserves ten intentional limitations. The payment reliability branch fixes the first group. Run on localhost with synthetic data only; no real payment service is connected.

## Current milestone

Sprint 0 is frozen as `baseline-v1`. This branch implements the first Rescue increment: durable payment attempts, idempotency, short database transactions and explicit provider timeouts. See the [before/after audit](docs/audit-report.md), [payment contract](docs/payment-contract.md) and [backlog](docs/backlog.md).

## Run

Requirements: JDK 21, Maven 3.9+, PostgreSQL 17 (Docker Compose is the standard local option).

The original Windows checkout also has [portable tool and start/stop instructions](docs/windows-local.md).

```sh
docker compose up -d --wait
mvn clean verify
mvn spring-boot:run
```

The API listens on `http://127.0.0.1:8080`; PostgreSQL is exposed only on `127.0.0.1:55432`. This branch uses a **fresh `backend_rescue_payments` database and separate Compose volume**, preserving the baseline database. The `backend_rescue` user/password are disposable local demo values, not real credentials. Do not reuse them elsewhere. An existing baseline schema is not upgraded automatically; versioned migrations remain pending.

PowerShell end-to-end check, in another terminal:

```powershell
./scripts/smoke.ps1
```

This creates a synthetic order, pays through the local HTTP simulator, then verifies the persisted status and list response. Each run creates another order; it does not clear existing data.

Optional environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`, `PAYMENT_PROVIDER_BASE_URL`, `PAYMENT_PROVIDER_CONNECT_TIMEOUT_MS` (default 1000), `PAYMENT_PROVIDER_READ_TIMEOUT_MS` (default 2000). If changing `PORT`, also set the provider URL to `http://127.0.0.1:<port>/sandbox-provider`.

Stop the application with Ctrl+C, then `docker compose stop`. The named database volume is retained.

## API

| Method | Path | Successful result |
| --- | --- | --- |
| POST | `/api/orders` | 201, order with `CREATED` status and Location header |
| GET | `/api/orders/{id}` | 200, order and payment count |
| GET | `/api/orders` | 200, all orders |
| POST | `/api/orders/{id}/payments` | 201 for a new authorization, 200 for a completed replay, 202 for a pending attempt |

Create order payload:

```json
{"customerEmail":"demo@example.com","totalAmount":25.50}
```

Payment payload (also send `Idempotency-Key: demo-payment-001`; reuse the same key only for the same order/amount):

```json
{"amount":25.50}
```

`POST /sandbox-provider/charges` is a **test fixture**, not an additional business feature. It returns a fresh fake charge ID on each HTTP request.

An uncertain provider outcome returns 503 and requires reconciliation; the same key never submits another charge. Another key cannot bypass an unresolved attempt. This contract intentionally changes the baseline; see [details and failure limits](docs/payment-contract.md).

## Design and deliberate limitations

Controllers → payment coordinator → short transactions / HTTP provider / short transactions. [Architecture](docs/architecture.md) explains the boundaries.

The baseline's ten intentional findings remain recorded in [findings](docs/findings.md). Payment replay/concurrency and provider failure cases now have regression tests. Order input validation, N+1, cross-API error consistency, authentication, schema migrations and complete operational logging remain pending. No production-readiness claim is made.

Run `mvn clean verify` for the H2 suite. For PostgreSQL, create a disposable `backend_rescue_test` database in the local cluster and run `scripts/verify-postgres.ps1`. That command recreates test tables: never target application data. CI runs both H2 and PostgreSQL 17. Tests include HTTP failures/timeouts, concurrent requests, persisted uncertainty and transactional rollback. Testcontainers lifecycle management remains future work; PostgreSQL CI currently uses a GitHub Actions service.

## Workflow

`baseline` and `baseline-v1` preserve the verified before state. `rescue/payment-reliability` contains this increment. The initial reproduction commit has six deliberately failing tests against baseline code; the following implementation makes them pass and extends regression coverage. Do not rewrite the baseline tag.

Source code is original demo work. No employer code, documents, infrastructure or real customer data are used.
