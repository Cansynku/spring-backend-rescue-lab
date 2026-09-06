# Order validation and list query — BR-004 / BR-005

This increment builds on schema migrations at `c629238`. It changes order HTTP input validation and the list query; it does not change the database schema or payment flow.

## Contract

`POST /api/orders` requires a nonblank, syntactically valid email of at most 255 characters and a positive decimal amount with at most 17 integer digits and two fractional digits, matching the payment API limit. The maximum is `99999999999999999.99`; database storage remains NUMERIC(38,2). Invalid or absent fields and malformed JSON return 400 `application/problem+json` with `code: INVALID_ORDER_REQUEST`; the response does not echo rejected values. Email syntax validation does not verify mailbox ownership or delivery. No email normalization is introduced.

This is intentionally stricter than the previous endpoint: requests that previously stored missing/invalid values, exceeded the payable limit or rounded excess fractional digits now fail. Consumers relying on those inputs must change. No external consumer inventory is available. Valid requests retain 201, Location and the existing response fields. Amounts 0.01 and the maximum payable value are tested. Legacy amounts above the payment limit remain readable and unchanged, but cannot be paid through the current API; their treatment requires an explicit domain decision, not rounding, splitting or automatic rewriting.

`GET /api/orders/{id}` returns 404 with `code: ORDER_NOT_FOUND` for an absent UUID, matching the payment missing-order code. Malformed UUIDs return 400 `INVALID_ORDER_REQUEST`. These order errors use ProblemDetail; complete cross-API error normalization remains separate work.

The list remains an unpaginated JSON array with no guaranteed ordering. A grouped left join returns one row per order and counts every payment status, including uncertain attempts. Orders without payments receive zero. Existing nullable legacy values remain readable; this increment neither rewrites them nor adds database constraints. Creation validation applies at the HTTP boundary.

## Query choice

The list selects response fields and a payment count in one database statement. It does not initialize payment entities or collections. Tests clear Hibernate statistics after fixture creation and invoke the service through its transaction boundary, without an enclosing test transaction. They assert one statement with one order and again with eleven orders, including zero and multiple payments. This proves a bounded statement count, not a latency benchmark or readiness for arbitrarily large lists. Pagination/index analysis remains future work.

## Reproduction and evidence

The test-only commit `9197046` ran against the parent implementation on H2: 16 cases, 12 failures, 2 errors, 2 passes. Invalid inputs were accepted or reached storage errors; one listed order required two statements instead of one. This is a preserved failing checkpoint, not the branch's final state.

After the fix, the PostgreSQL 17 clean build passes all 51 tests with no failures, errors or skips. The H2 clean build discovers 51 tests: 47 pass, and 4 PostgreSQL-only migration cases are intentionally skipped. See the PR checks for the final remote runs. The added cases cover invalid inputs, storage boundaries, stable error codes, empty arrays, list cardinality, payment counts and legacy readability.

The build uses an ignored isolated checkout because the existing local demo holds its JAR open. No application restart, baseline migration or original data modification is part of this increment. The previously blocked packaged-app migration smoke remains pending in [schema-migrations.md](schema-migrations.md).

## Remaining scope

REV-01 validation: the new upper-bound rejection test first failed against `e6b9987` (201 instead of 400). After aligning limits, the full PostgreSQL build passes 54 tests; H2 passes 50 with 4 intentional PostgreSQL-only skips. Added coverage checks maximum create/pay/replay with exact stored amounts, rejection of the next cent and unchanged legacy reads.

Authentication/ownership and reconciliation require domain decisions. Cross-API error normalization, fuller operational logging and Testcontainers lifecycle remain pending. This is an educational demo, not a production payment service.
