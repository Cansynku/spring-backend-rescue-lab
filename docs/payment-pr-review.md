# Payment PR #1 — review checkpoint

Reviewed the payment increment at `64ea86358a02a09f4953ddab46642279988fe29f` against `baseline-v1`.

The important change is the transaction boundary: a PENDING attempt commits before HTTP, then order/payment completion commits together. A replay reuses the stored attempt. The provider call does not hold the order row lock, and uncertain outcomes block a new submission. Regression evidence is in [audit-report.md](audit-report.md).

Suitable as a **draft educational increment**, with the breaking Idempotency-Key and status contract disclosed. It is not a production-ready payment integration and has not been merged. The principal follow-ups are unmanaged database migration (this branch), explicit reconciliation/crash recovery, authentication/ownership and the remaining baseline findings. Successful tests do not resolve those domain decisions.

The migration branch is based on PR #1 to keep its diff focused. Review/merge the parent separately; no merge is authorized or performed as part of this checkpoint.
