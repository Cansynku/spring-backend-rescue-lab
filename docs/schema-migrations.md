# Versioned schema migrations — BR-009

Flyway owns schema creation/evolution; Hibernate runs with `ddl-auto=validate`. Automatic baselining is disabled, migration checksums are validated, and Flyway clean is disabled. No application business contract changes in this increment.

## Versions

| Version | Purpose |
| --- | --- |
| V1 | Original Orders/Payments schema, matching the observed baseline-v1 PostgreSQL columns and constraints |
| V2 | Nullable unique idempotency key and PENDING/UNKNOWN payment states |

PostgreSQL and H2 have separate migration locations because Hibernate maps enums differently. PostgreSQL is the upgrade acceptance database; H2 is for fast regression checks.

An empty database migrates V1 → V2 automatically. Starting again applies zero migrations. Never edit an applied migration; add V3, V4, etc. Historical payment keys remain NULL; inventing a key would incorrectly imply a recoverable client retry identity.

## Existing unmanaged PostgreSQL database

Normal startup **refuses** a nonempty database without Flyway history. There are two explicit, narrow adoption modes:

- `--lab.schema-adoption=baseline-v1`: verify the original Hibernate schema, register baseline version 1 and apply V2.
- `--lab.schema-adoption=payment-v2`: verify the payment increment schema and register version 2 without reapplying its changes. Existing keys and outcomes are preserved.

The guard compares exactly the supported table/column definitions (including precision, nullability and defaults), primary/foreign/check/unique constraints, validated/immediate enforcement, the expected payment status constraint name, and absence of triggers. This is a known-schema adoption guard, not a general schema-diff or security audit. Unexpected versions/shapes fail before creating history.

### Procedure

1. Stop writers and the old application through the normal operating procedure. Do not run old and new code against the same database during upgrade. Take and verify a backup; test this procedure on a copy first.
2. Confirm the database URL and which of the two supported schema versions it contains. Use normal startup for empty or already managed databases.
3. For an unmanaged supported database, start the packaged application once with the corresponding adoption option and the explicit target database URL, for example:

```sh
java -jar target/spring-backend-rescue-lab-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:postgresql://127.0.0.1:55432/REVIEW_COPY \
  --lab.schema-adoption=baseline-v1
```

`REVIEW_COPY` is a placeholder, not a production target. Configure database credentials through the environment. The application must have exclusive maintenance access during guard/baseline/migrate: the preflight check and Flyway operation are separate connections, not one atomic cross-connection lock.

4. Confirm successful Flyway history, Hibernate validation, preserved rows/values and the API smoke. Remove the adoption option from subsequent starts. Leaving it enabled deliberately fails when history already exists.
5. If a migration fails after the baseline marker was recorded, inspect the failure/history and fix the cause; do not delete history or use automatic repair as a shortcut. PostgreSQL executes these migrations transactionally, but adoption and migration are separate operations. Resume with normal migration once the cause is resolved.

There is no destructive down migration. Rollback means restoring a verified backup and a compatible application version. Do not run baseline-v1 code against a V2 schema containing new payment states.

## Evidence (2026-09-06)

- Full PostgreSQL 17.11 build: **35 tests, 0 failures/errors/skipped**. After adding an explicit assertion that an existing key survives V2 adoption, all **7 migration tests** passed again on PostgreSQL.
- Final H2 clean build: **35 tests discovered, 31 executed, 4 PostgreSQL-only adoption cases skipped**, no failures/errors. These skips are intentional and those cases run in the PostgreSQL CI job.
- Upgrade fixture is an independent snapshot of the actual baseline-v1 Hibernate DDL, with authorized/failed payments, nullable legacy values and existing orders. Tests compare all original column values before/after; they also check preserved keys, new status values, uniqueness, second-run no-op, malformed-schema rejection and checksum mismatch rejection.
- Existing payment behavior still passes with Hibernate validation against Flyway-created tables.
- Local build ran in an ignored isolated source copy because Windows locks the JAR used by the existing demo. No running application was stopped.
- A copy named `backend_rescue_migration_review` was created from the original local baseline database. Automatic approval review blocked starting the new application against that copy. **That copy has not been adopted/migrated, and packaged-application startup on the copied database remains unverified.** The original baseline and running payment demo were not migrated.

## Test isolation

`scripts/verify-postgres.ps1` targets only `backend_rescue_test`. Tests now use migrations and Hibernate validation; they no longer recreate application tables with Hibernate. Individual migration cases create unique `migration_test_*` schemas and remove only those test-owned schemas. Other application tests clear their own order/payment fixtures. Never point tests at application data.

## Remaining work

Complete the blocked packaged-app check on a baseline copy when normal execution is available. N+1, order validation, consistent cross-API errors, authentication/ownership and reconciliation remain separate increments. The adoption paths are validated for the two observed PostgreSQL 17 schemas, not arbitrary customer databases.
