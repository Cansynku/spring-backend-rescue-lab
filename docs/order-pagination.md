# Order pagination

GET /api/orders/page?page=0&size=20 returns items, page, size, totalElements and totalPages. Page numbering starts at zero; size defaults to 20 and accepts 1–100. Negative pages, malformed numbers and offsets above the JPA integer range return HTTP 400 ProblemDetail. Empty and out-of-range pages return an empty items array with collection totals.

The database limits grouped summaries ordered by UUID, with one row per order and complete payment counts. A separate query counts orders rather than joined payments. Ordering is stable for an unchanged collection, not chronological. Concurrent inserts/deletes may shift offset pages; this is not a snapshot.

GET /api/orders remains the compatible, unbounded array endpoint. The demo uses 20-row pages and Previous/Next controls. The last order created in this browser session stays visible even outside the current page, fetched by ID, so creating, paying and replaying still work. Visible counters include that order (at most 21 rows); the navigation shows collection totals. Reloading the browser clears the pinned order but preserves existing payment keys.

Acceptance coverage: static pages without duplicates, accurate totals/payment counts, empty pages, invalid parameters, navigation boundaries and paying a newly created order outside the page. See OrderReliabilityTest and scripts/test-demo-ui.cjs.

No schema, dependency, authentication or provider changes. Production still needs an explicit decision about volume and compatibility of the unbounded legacy route.
