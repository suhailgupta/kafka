-- Analytics database schema - JDBC sink writes Debezium payload here
-- created_at as TEXT: Debezium sends ZonedTimestamp as ISO-8601 string; cast to timestamptz in queries if needed

CREATE TABLE IF NOT EXISTS order_events (
    id BIGINT PRIMARY KEY,
    order_number VARCHAR(50),
    customer_id VARCHAR(100),
    customer_email VARCHAR(255),
    total_amount DECIMAL(19, 2),
    currency VARCHAR(3),
    status VARCHAR(20),
    created_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_order_events_customer_id ON order_events(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_events_created_at ON order_events(created_at);
