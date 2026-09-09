# External Health Check pilot: Alf.io

Reviewed 2026-09-08, read-only, at commit 6296c0c855b13050a19ad111f88bc804ab85bdaf of [alfio-event/alf.io](https://github.com/alfio-event/alf.io/tree/6296c0c855b13050a19ad111f88bc804ab85bdaf). Alf.io is third-party open-source software; no authorship or upstream contribution is claimed.

## Question and evidence

[Issue #1475](https://github.com/alfio-event/alf.io/issues/1475) reports mandatory supplements missing from manually created reservations. It was open when checked. Its comment points to the broader options/donations request #1462; that feature expansion is excluded here.

The reviewed admin creation path in [AdminReservationManager](https://github.com/alfio-event/alf.io/blob/6296c0c855b13050a19ad111f88bc804ab85bdaf/src/main/java/alfio/manager/AdminReservationManager.java#L574) reserves tickets without invoking the supplement booking component. The public path in [TicketReservationManager](https://github.com/alfio-event/alf.io/blob/6296c0c855b13050a19ad111f88bc804ab85bdaf/src/main/java/alfio/manager/TicketReservationManager.java#L355) invokes it. [AdditionalServiceManager](https://github.com/alfio-event/alf.io/blob/6296c0c855b13050a19ad111f88bc804ab85bdaf/src/main/java/alfio/manager/AdditionalServiceManager.java#L391) selects mandatory saleable supplements automatically.

This confirms a code-path difference consistent with the report, not a newly executed reproduction or a proven production loss. Subsequent payment-link behavior still needs verification. Mandatory-supplement semantics for administrative exceptions require maintainer agreement.

## Proposed acceptance test

Use synthetic fixtures, without sending email or taking payments. For two tickets priced at 100, with taxes/discounts excluded to isolate the calculation, compare public and admin reservations with a mandatory fixed supplement of 5 per ticket: expected total 210 if that contract is agreed. Repeat with a 10% supplement per ticket (220), and no supplement (200). Inspect persisted items and the payment-link total; reading the link must not duplicate supplements. A future fix also needs rollback coverage when supplement booking fails.

These are proposed expectations, not executed results. Existing percentage integration tests exercise the public manager; no supplement coverage was found in the admin integration-test class inspected, without claiming an exhaustive test inventory.

## Execution limit and value

The integration configuration starts PostgreSQL and stripe-mock through Testcontainers. No usable container runtime was established in the checked environment. No tests, services, databases or installations were run for this pilot, and no report or PR was sent upstream. Existing Backend Rescue databases were not reused.

The deliverable is a bounded review with an immutable source reference and a falsifiable next test. It validates part of the review method, not customer demand, an accepted fix or a commercial engagement.
