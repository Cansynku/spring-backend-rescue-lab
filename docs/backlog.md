# Sprint 0 and Rescue backlog

Current execution scope: BRL-001 through BRL-005; functional baseline, then freeze. The broader business plan is context, not proof of revenue or market validation.

| Ticket | Deliverable | Status |
| --- | --- | --- |
| BRL-001 | Java 21 / Spring Boot 3.5.16 scaffold | Implemented |
| BRL-002 | Orders and payments, four business endpoints | Implemented |
| BRL-003 | Simulated HTTP provider integration | Implemented |
| BRL-004 | Baseline happy-path tests | Implemented; result in validation.md |
| BRL-005 | PostgreSQL smoke, local Git, baseline-v1, GitHub | Verified and released as baseline-v1 |
| BRL-006 | Reproduce and audit each seeded finding | Pending |
| BRL-007 | Small independent Rescue fixes | Pending |
| BRL-008 | Failure/concurrency tests and PostgreSQL Testcontainers | Pending |
| BRL-009 | GitHub Actions baseline CI | Passed on GitHub, Java 21 / Linux; see validation.md |
| BRL-010 | Verified before/after README | Before documented; after pending |
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

Start BRL-006 from the frozen before state. Prioritize BR-001 and BR-002 together when defining payment failure semantics, but keep reproductions and fixes reviewable. No claims of production readiness before the Rescue checks are complete.
