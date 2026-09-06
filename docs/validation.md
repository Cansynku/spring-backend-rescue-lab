# Validation evidence

Verified locally on 2026-09-06, Windows 11.

| Check | Observed result |
| --- | --- |
| JDK | Eclipse Temurin 21.0.12.1+1, portable project-local distribution; SHA-256 matched the vendor API checksum |
| Maven | 3.9.11 |
| Spring Boot | 3.5.16 resolved from Maven Central |
| `mvn -B -ntp clean verify` | BUILD SUCCESS; 1 minute 12 seconds |
| Automated baseline tests | 3 executed, 0 failures, 0 errors, 0 skipped |
| PostgreSQL | 17.11 portable vendor binaries; dedicated `backend_rescue` database |
| API smoke script | PASS: create order 201, create payment 201, persisted PAID status, paymentCount 1, order present in listing |
| Network binding | Application 127.0.0.1:8080; PostgreSQL 127.0.0.1:55432 |
| Editor | VS Code window observed with spring-backend-rescue-lab in its title |

## Reproduction and limits

- `mvn clean verify` executes the three H2 happy-path tests, including actual HTTP to a separate test provider. It does **not** run PostgreSQL integration tests.
- `scripts/smoke.ps1` was executed against the packaged application and PostgreSQL with the local sandbox provider. Two synthetic orders were created during verification; the first run exposed an array handling error in the PowerShell script, which was corrected before a successful rerun. No application fix was needed.
- Docker is not installed on this machine, so `compose.yaml` has not been executed locally. PostgreSQL was verified through its portable distribution instead. Windows blocked the auxiliary `createdb.exe`; a normal JDBC connection created the isolated database, without changing Windows security settings.
- Portable tools and runtime data live in ignored `.tools/` and `.local/`. They are not published. The machine-wide Java configuration is unchanged.
- No failure, concurrency, idempotency, authorization or query-count regression tests have been claimed. Those remain Rescue work.
- Remote CI will be checked after publication. A local green build alone is not remote CI evidence.
