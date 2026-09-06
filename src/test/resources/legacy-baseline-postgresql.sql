-- Independent snapshot of baseline-v1 Hibernate DDL, verified against the original local database.
CREATE TABLE purchase_orders (
    id UUID NOT NULL PRIMARY KEY,
    customer_email VARCHAR(255),
    status VARCHAR(255),
    total_amount NUMERIC(38,2),
    CONSTRAINT purchase_orders_status_check CHECK (status IN ('CREATED','PAID','CANCELLED'))
);
CREATE TABLE payments (
    id UUID NOT NULL PRIMARY KEY,
    amount NUMERIC(38,2),
    provider_payment_id VARCHAR(255),
    status VARCHAR(255),
    order_id UUID NOT NULL,
    CONSTRAINT payments_status_check CHECK (status IN ('AUTHORIZED','FAILED')),
    CONSTRAINT fkkn6soeqfwhh4yi468p39y9mrw FOREIGN KEY (order_id) REFERENCES purchase_orders(id)
);
INSERT INTO purchase_orders VALUES ('00000000-0000-0000-0000-000000000001','legacy@example.com','PAID',25.50);
INSERT INTO purchase_orders VALUES ('00000000-0000-0000-0000-000000000002',NULL,'CREATED',NULL);
INSERT INTO payments VALUES ('00000000-0000-0000-0000-000000000003',25.50,'sandbox-legacy','AUTHORIZED','00000000-0000-0000-0000-000000000001');
INSERT INTO payments VALUES ('00000000-0000-0000-0000-000000000004',NULL,NULL,'FAILED','00000000-0000-0000-0000-000000000002');
