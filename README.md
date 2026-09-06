# Spring Backend Rescue Lab

An intentionally imperfect Java/Spring Boot Orders & Payments API. The goal is to demonstrate **finding → evidence → impact → fix → verification** using small, reviewable changes.

> **Educational baseline — not production-ready.** The ten documented limitations are deliberate. Run on localhost with synthetic data only. Payments are simulated; no real payment service is connected.

## Current milestone

Sprint 0 implements the functional **before** state. The Rescue and its **after** state have not been implemented. See [backlog](docs/backlog.md), [findings](docs/findings.md) and [validation](docs/validation.md) for actual status.

## Run

Requirements: JDK 21, Maven 3.9+, PostgreSQL 17 (Docker Compose is the standard local option).

The original Windows checkout also has [portable tool and start/stop instructions](docs/windows-local.md).

```sh
docker compose up -d --wait
mvn clean verify
mvn spring-boot:run
```

The API listens on `http://127.0.0.1:8080`; PostgreSQL is exposed only on `127.0.0.1:55432`. The `backend_rescue` database/user/password are **disposable local demo values**, not real credentials. Do not reuse them elsewhere. Ports are selected to avoid the usual local PostgreSQL port.

PowerShell end-to-end check, in another terminal:

```powershell
./scripts/smoke.ps1
```

This creates a synthetic order, pays through the local HTTP simulator, then verifies the persisted status and list response. Each run creates another order; it does not clear existing data.

Optional environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`, `PAYMENT_PROVIDER_BASE_URL`. If changing `PORT`, also set the provider URL to `http://127.0.0.1:<port>/sandbox-provider`.

Stop the application with Ctrl+C, then `docker compose stop`. The named database volume is retained.

## API

| Method | Path | Successful result |
| --- | --- | --- |
| POST | `/api/orders` | 201, order with `CREATED` status and Location header |
| GET | `/api/orders/{id}` | 200, order and payment count |
| GET | `/api/orders` | 200, all orders |
| POST | `/api/orders/{id}/payments` | 201, simulated authorized payment; order becomes `PAID` |

Create order payload:

```json
{"customerEmail":"demo@example.com","totalAmount":25.50}
```

Payment payload:

```json
{"amount":25.50}
```

`POST /sandbox-provider/charges` is a **test fixture**, not an additional business feature. It returns a fresh fake charge ID on each HTTP request.

## Design and deliberate limitations

Controllers → transactional services → JPA/PostgreSQL; the payment service calls a synchronous HTTP provider. [Architecture](docs/architecture.md) explains the boundaries.

The baseline intentionally has no idempotency, broad payment transactions, no explicit HTTP timeouts, missing validation, lazy collection N+1 queries, inconsistent errors, happy-path-only H2 tests, open authorization, automatic schema updates and insufficient application logging. [Findings](docs/findings.md) separates code evidence from behavior still requiring reproduction.

Three automated tests cover order creation/read, listing and successful payment over a real local HTTP connection with database persistence. **Passing these tests does not establish production readiness or PostgreSQL equivalence.** PostgreSQL smoke verification is recorded separately. Testcontainers, failure scenarios, migrations and security corrections belong to Rescue.

## Workflow

`baseline` contains the original implementation; `baseline-v1` freezes the verified before state. Future fixes should use separate branches and small commits tied to `BR-001` … `BR-010`, with a failing reproduction before each fix. Do not rewrite the baseline tag.

Source code is original demo work. No employer code, documents, infrastructure or real customer data are used.
