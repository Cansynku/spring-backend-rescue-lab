CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY,
    customer_email VARCHAR(255),
    status ENUM ('CREATED', 'PAID', 'CANCELLED'),
    total_amount NUMERIC(38,2)
);
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    amount NUMERIC(38,2),
    provider_payment_id VARCHAR(255),
    status ENUM ('AUTHORIZED', 'FAILED'),
    order_id UUID NOT NULL,
    CONSTRAINT payments_order_fk FOREIGN KEY (order_id) REFERENCES purchase_orders(id)
);
