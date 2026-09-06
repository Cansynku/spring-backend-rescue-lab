CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY,
    customer_email VARCHAR(255),
    status VARCHAR(255),
    total_amount NUMERIC(38,2),
    CONSTRAINT purchase_orders_status_check CHECK (status IN ('CREATED', 'PAID', 'CANCELLED'))
);
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    amount NUMERIC(38,2),
    provider_payment_id VARCHAR(255),
    status VARCHAR(255),
    order_id UUID NOT NULL,
    CONSTRAINT payments_status_check CHECK (status IN ('AUTHORIZED', 'FAILED')),
    CONSTRAINT fkkn6soeqfwhh4yi468p39y9mrw FOREIGN KEY (order_id) REFERENCES purchase_orders(id)
);
