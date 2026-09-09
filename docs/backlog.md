# Sprint 0 and Rescue backlog

Baseline BRL-001 through BRL-005 is frozen. Current local milestone: demo presentation on rescue/demo-interface, including the prior payment, migration, order and observability increments; no parent PR merge. See [order evidence](order-reliability.md). The broader business plan is context, not proof of revenue or market validation.

| Ticket | Deliverable | Status |
| --- | --- | --- |
| BRL-001 | Java 21 / Spring Boot 3.5.16 scaffold | Implemented |
| BRL-002 | Orders and payments, four business endpoints | Implemented |
| BRL-003 | Simulated HTTP provider integration | Implemented |
| BRL-004 | Baseline happy-path tests | Implemented; result in validation.md |
| BRL-005 | PostgreSQL smoke, local Git, baseline-v1, GitHub | Verified and released as baseline-v1 |
| BRL-006 | Reproduce and audit each seeded finding | Payment, order validation and list-query failures reproduced; other findings pending |
| BRL-007 | Small independent Rescue fixes | Payment reliability, migrations, order validation and list query implemented; remaining findings pending |
| BRL-008 | Failure/concurrency tests and PostgreSQL Testcontainers | Failure/concurrency tests run on H2 and real PostgreSQL; Testcontainers lifecycle pending |
| BRL-009 | GitHub Actions CI | H2 and PostgreSQL workflow prepared for this branch; baseline CI evidence retained |
| BRL-010 | Verified before/after README | Payment before/after documented; complete Rescue pending |
| BRL-011 | Reusable Backend Health Check checklist | Completed locally 2026-09-07: [checklist](backend-health-check-checklist.md), [report template](backend-health-check-report-template.md), [lab example](backend-health-check-lab-example.md); evidence and local links reviewed. Not a production certification; second-repository pilot pending |
| BRL-012 | Scoped Backend Health Check offer | Local draft in backend-health-check-offer.md; pricing, timing and market validation pending; not sent |
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

Visible demo: `rescue/demo-interface` adds the local Spanish screen, real HTTP flow coverage, dependency-free JavaScript checks and a manual Windows launcher. [Setup and limitations](demo-screen.md). The user opened the WSL PostgreSQL demo on 8084 on 2026-09-07: GET / 200 and smoke PASS (201/201/200, PAID, one payment), as recorded in the continuation handoff. Browser creation/payment/replay and visual inspection passed on 2026-09-07; see [demo closure and presentation](demo-presentation.md). No public deployment or merge.

REV-03/REV-04 follow-up: `rescue/safe-observability` adds replacement of raw Hibernate SQL diagnostics, server-owned request correlation and redaction/context-isolation tests. See [operational scope](observability.md) and [presentation report](project-progress.md). The historical review status below describes the parent PR; verification and delivery state for the follow-up belong to its own PR. The offer and reusable method are now local drafts. Final combined stack acceptance remains pending; the separate packaged migration check remains unverified and is not required to present the local order/payment demo.

Combined review completed at PR 3 head `e6b9987`: [findings and acceptance criteria](stack-review.md). REV-01 aligns payable amounts. REV-02 adds malformed-payment-ID ProblemDetail, removes invalid path values from client-error bodies, and distinguishes verified key collisions from reservation storage failures. At that historical head REV-03 (framework error-detail leakage) and REV-04 (correlation and safe events) were specified but unimplemented; the later safe-observability increment addresses them as documented above. Complete the packaged-app migration check when execution is available (see schema-migrations.md). Authentication and payment reconciliation need explicit domain contracts. No production-readiness claim before those checks are complete.
