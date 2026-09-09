ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE payments ADD CONSTRAINT payments_idempotency_key_unique UNIQUE (idempotency_key);
ALTER TABLE payments ALTER COLUMN status ENUM ('PENDING', 'AUTHORIZED', 'FAILED', 'UNKNOWN');
