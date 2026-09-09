# Safe diagnostics and request correlation — REV-03 / REV-04

## Behavior and field policy

Every synchronous servlet request receives a server-generated UUID in `X-Request-ID`. Incoming values of that header are ignored. The UUID is placed in logging MDC and appears in the console pattern. The filter clears its context in `finally` (or restores an enclosing context), including exceptional exits. This is local request correlation, not distributed tracing or propagation to background workers/the provider.

`request_finished` records only response status and completion/exception outcome; an unhandled exception uses `status=unresolved` because error dispatch has not established its final HTTP status yet. It never records the URL, query string, headers or body. Payment events distinguish authorization, replay, expected rejection, verified key collision, uncertainty and failed persistence. Their fields are application-owned payment UUID, finite status/error code, bounded category or exception class name; never exception text.

A Logback TurboFilter replaces messages from Hibernate's exact `org.hibernate.engine.jdbc.spi.SqlExceptionHelper` logger before they reach appenders. Warning/error diagnostics produce `database_operation_failed diagnostic=redacted` instead of the database's message and arguments. Other logger families are not suppressed. This preserves a signal while deliberately discarding SQL text, constraint detail and submitted values. One database failure can produce several generic events; do not count these as unique incidents.

The filter is installed by `logback-spring.xml`; the current application uses console logging. Alternate logging configurations must reinstall the filter and rerun the redaction checks. This is a fix for the reproduced Hibernate diagnostic path, not a general scrubber for arbitrary logs, future exception stack traces, database-server logs, debug HTTP/body logging or third-party appenders. Authentication and secrets management remain separate concerns.

## Acceptance evidence

- `frameworkAndProviderFailureLogsExcludeSensitiveValues`: HTTP provider failure plus a deterministic real database unique violation; captured output contains a generic database event, uncertain payment event and the response's request ID, but excludes a synthetic key, email and provider body.
- `simultaneousDifferentOrdersCannotClaimTheSameKey`: concurrent requests still yield one authorization and one conflict, one payment/provider call, a safe rejection event and no raw race key in captured logs.
- `RequestCorrelationTest`: client header is ignored; the UUID is present during processing; context is cleared after an exception; simultaneous requests have distinct IDs; reused worker threads start/end clean.
- Existing replay, rollback, storage-error and migration suites remain in the complete verification. Failure fixtures never contact a real payment service.

Run `mvn clean verify` for H2 or `scripts/verify-postgres.ps1` against the dedicated disposable PostgreSQL database. Never run tests against application data. Evidence is summarized in [the presentation report](project-progress.md); raw local logs remain ignored rather than published.

## Interpreting an incident

Use the returned `X-Request-ID` to find the request and payment events in the application console. `PAYMENT_OUTCOME_UNKNOWN` or a durable `PENDING` payment still requires reconciliation; correlation does not authorize resubmission or resolve the provider outcome. A generic database event plus `payment_reservation_failed` identifies the failure stage without publishing SQL values. No automatic reconciliation or production monitoring was added.
