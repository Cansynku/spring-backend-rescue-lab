# Sprint 0 and Rescue backlog

Baseline BRL-001 through BRL-005 is frozen. Current increment: BR-009 versioned migrations, based on the payment reliability branch; no parent PR merge. The broader business plan is context, not proof of revenue or market validation.

| Ticket | Deliverable | Status |
| --- | --- | --- |
| BRL-001 | Java 21 / Spring Boot 3.5.16 scaffold | Implemented |
| BRL-002 | Orders and payments, four business endpoints | Implemented |
| BRL-003 | Simulated HTTP provider integration | Implemented |
| BRL-004 | Baseline happy-path tests | Implemented; result in validation.md |
| BRL-005 | PostgreSQL smoke, local Git, baseline-v1, GitHub | Verified and released as baseline-v1 |
| BRL-006 | Reproduce and audit each seeded finding | Payment increment reproduced; other findings pending |
| BRL-007 | Small independent Rescue fixes | Payment reliability and versioned migrations implemented; remaining findings pending |
| BRL-008 | Failure/concurrency tests and PostgreSQL Testcontainers | Failure/concurrency tests run on H2 and real PostgreSQL; Testcontainers lifecycle pending |
| BRL-009 | GitHub Actions CI | H2 and PostgreSQL workflow prepared for this branch; baseline CI evidence retained |
| BRL-010 | Verified before/after README | Payment before/after documented; complete Rescue pending |
| BRL-011 | Reusable production readiness checklist | Pending |
| BRL-012 | Scoped Backend Health Check offer | Pending |
| BRL-013 | Opportunity log with verified current leads | Pending |
| BRL-014 | First client proposals | Pending; sending requires explicit authorization |

## Baseline acceptance

- Application starts with PostgreSQL and all four business endpoints work.
- Simulated HTTP provider is actually called and the result is persisted.
- `mvn clean verify` succeeds on Java 21.
- Ten intentional findings documented; no real secrets or employer material.
- Local source opened in VS Code, committed and frozen as `baseline-v1`.
- Repository published to the owner's GitHub and remote state verified.

## Next milestone

Next increment: reproduce order validation/error and N+1 cases. Complete the packaged-app migration check when execution is available (see schema-migrations.md). Authentication and payment reconciliation need explicit domain contracts. No production-readiness claim before those checks are complete.
