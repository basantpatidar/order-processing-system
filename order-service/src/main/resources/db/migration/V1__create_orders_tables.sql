-- V1__create_orders_tables.sql

CREATE TABLE orders (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   VARCHAR(100) NOT NULL,
    status        VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    total_amount  NUMERIC(12, 2) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  VARCHAR(100) NOT NULL,
    quantity    INTEGER NOT NULL CHECK (quantity > 0),
    price       NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    subtotal    NUMERIC(12, 2) GENERATED ALWAYS AS (quantity * price) STORED
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status      ON orders(status);
CREATE INDEX idx_order_items_order  ON order_items(order_id);
