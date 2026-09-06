ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE payments ADD CONSTRAINT payments_idempotency_key_unique UNIQUE (idempotency_key);
ALTER TABLE payments DROP CONSTRAINT payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING', 'AUTHORIZED', 'FAILED', 'UNKNOWN'));
-- Legacy payments intentionally keep a NULL key: no synthetic retry identity is invented.
