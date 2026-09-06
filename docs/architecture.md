# Architecture — payment reliability increment

```mermaid
flowchart LR
    Client -->|localhost HTTP| Controller[Order / Payment controllers]
    Controller --> Service[Payment coordinator: no transaction]
    Service --> Transactions[Short reserve / complete transactions]
    Transactions --> JPA[JPA repositories]
    JPA --> PG[(PostgreSQL 17)]
    Service -->|HTTP outside DB transaction| Provider[Local sandbox payment provider]
```

- Java 21, Spring Boot 3.5.16, Maven; package `dev.javiercano.backendrescue`.
- `order`: order aggregate, DTOs, repository, service and controller.
- `payment`: durable payment intent, idempotency constraint, DTOs, coordinator, transactional boundaries, payment errors and controller.
- `provider`: RestClient integration and a local simulator; no real money moves.
- `config`: intentionally permissive Spring Security policy.
- Database: `purchase_orders` → one-to-many `payments`; UUID identifiers, decimal amounts and unique payment idempotency keys. PENDING intents commit before HTTP; AUTHORIZED commits with the PAID order; ambiguous failures preserve UNKNOWN or PENDING. Currency/refunds/cancellation APIs are outside scope.
- `open-in-view=false`: response mapping happens inside service transactions. Lazy payment collection access still causes N+1 on a list.
- The local simulator lives in the same server but is called through an actual HTTP connection. Tests replace its URL with a separate HTTP fixture on a random local port.
- Business endpoints and database bind to loopback by default. The permissive application security is still an intentional flaw; localhost containment is not a production security model.

There are no queues, cloud services, authentication server, frontend, AI API, paid dependencies or external payment credentials.

`PaymentService` rejects ambient transactions (`NEVER`). A separate proxied `PaymentTransactions` bean owns each `REQUIRES_NEW` transaction. Order row locking serializes reservation decisions, while a unique key protects cross-order races. HTTP does not hold the order lock. A crashed process can leave an unresolved intent: recovery is deliberately conservative, with no automatic resubmission. See [contract](payment-contract.md).

Spring Boot compatibility was checked against its [official system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) and the 3.5.16 artifact was verified in Maven Central. Versions are pinned in `pom.xml`; PostgreSQL Compose follows the 17 major line.
