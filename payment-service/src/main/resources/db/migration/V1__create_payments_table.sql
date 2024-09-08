-- V1__create_payments_table.sql

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        VARCHAR(100) NOT NULL UNIQUE,   -- UNIQUE enforces idempotency
    customer_id     VARCHAR(100) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    failure_reason  VARCHAR(255),
    processed_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id    ON payments(order_id);
CREATE INDEX idx_payments_customer_id ON payments(customer_id);
CREATE INDEX idx_payments_status      ON payments(status);
