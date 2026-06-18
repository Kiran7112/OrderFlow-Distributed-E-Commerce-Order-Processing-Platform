#!/bin/bash
set -e

PGUSER="${POSTGRES_USER:-postgres}"

echo ">>> Creating OrderFlow databases..."
for db in orders_db inventory_db payments_db shipping_db analytics_db; do
    psql -v ON_ERROR_STOP=1 --username "$PGUSER" --dbname="postgres" \
        -c "CREATE DATABASE $db;"
done

# ===================== ORDERS DB =====================
echo ">>> Initializing orders_db..."
psql -v ON_ERROR_STOP=1 --username "$PGUSER" --dbname="orders_db" <<'EOSQL'
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PLACED',
    total_amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_status CHECK (status IN ('PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'PAYMENT_FAILED'))
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_order_product UNIQUE(order_id, product_id)
);

CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    order_id UUID NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255)
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_processed_events_order_id ON processed_events(order_id);
CREATE INDEX idx_processed_events_event_id ON processed_events(event_id);
CREATE INDEX idx_order_audit_log_order_id ON order_audit_log(order_id);

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
EOSQL

# ===================== INVENTORY DB =====================
echo ">>> Initializing inventory_db..."
psql -v ON_ERROR_STOP=1 --username "$PGUSER" --dbname="inventory_db" <<'EOSQL'
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    category VARCHAR(100),
    sku VARCHAR(100) UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    available_qty INT NOT NULL CHECK (available_qty >= 0),
    reserved_qty INT NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
    reorder_level INT DEFAULT 10,
    version BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id),
    order_id UUID NOT NULL,
    reserved_qty INT NOT NULL,
    reservation_status VARCHAR(50) NOT NULL DEFAULT 'RESERVED',
    reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP,
    CONSTRAINT check_reservation_status CHECK (reservation_status IN ('RESERVED', 'RELEASED', 'CONFIRMED'))
);

CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    order_id UUID NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_stock_product_id ON stock(product_id);
CREATE INDEX idx_stock_reservations_order_id ON stock_reservations(order_id);
CREATE INDEX idx_stock_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX idx_stock_reservations_status ON stock_reservations(reservation_status);
CREATE INDEX idx_processed_events_order_id ON processed_events(order_id);

INSERT INTO products (name, description, price, category, sku) VALUES
    ('Laptop Pro', 'High-performance laptop with 16GB RAM', 1299.99, 'Electronics', 'LAPTOP-001'),
    ('Wireless Mouse', 'Ergonomic wireless mouse with USB receiver', 29.99, 'Accessories', 'MOUSE-001'),
    ('USB-C Cable', 'Premium USB-C charging cable 2m', 19.99, 'Accessories', 'CABLE-001'),
    ('Monitor 4K', '27-inch 4K Ultra HD monitor', 399.99, 'Electronics', 'MONITOR-001'),
    ('Mechanical Keyboard', 'RGB mechanical gaming keyboard', 149.99, 'Accessories', 'KEYBOARD-001');

INSERT INTO stock (product_id, available_qty, reserved_qty, reorder_level)
SELECT id, 100, 0, 10 FROM products;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
EOSQL

# ===================== PAYMENTS DB =====================
echo ">>> Initializing payments_db..."
psql -v ON_ERROR_STOP=1 --username "$PGUSER" --dbname="payments_db" <<'EOSQL'
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) DEFAULT 'USD',
    gateway_ref VARCHAR(255),
    gateway_name VARCHAR(50) DEFAULT 'MOCK',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED'))
);

CREATE TABLE payment_retries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    retry_at TIMESTAMP,
    retried_at TIMESTAMP,
    CONSTRAINT check_attempt CHECK (attempt_number > 0)
);

CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(255),
    gateway_ref VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    CONSTRAINT check_refund_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    order_id UUID NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_order_id ON transactions(order_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_payment_retries_transaction_id ON payment_retries(transaction_id);
CREATE INDEX idx_refunds_transaction_id ON refunds(transaction_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_processed_events_order_id ON processed_events(order_id);

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
EOSQL

# ===================== SHIPPING DB =====================
echo ">>> Initializing shipping_db..."
psql -v ON_ERROR_STOP=1 --username "$PGUSER" --dbname="shipping_db" <<'EOSQL'
CREATE TABLE shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    tracking_number VARCHAR(50) UNIQUE,
    carrier VARCHAR(50),
    carrier_account VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    estimated_delivery DATE,
    actual_delivery_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_status CHECK (status IN ('CREATED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'RETURNED', 'FAILED'))
);

CREATE TABLE shipment_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shipping_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    location VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shipping_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    recipient_name VARCHAR(255) NOT NULL,
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(2) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    order_id UUID NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shipments_order_id ON shipments(order_id);
CREATE INDEX idx_shipments_tracking_number ON shipments(tracking_number);
CREATE INDEX idx_shipments_status ON shipments(status);
CREATE INDEX idx_shipments_created_at ON shipments(created_at);
CREATE INDEX idx_shipment_items_shipment_id ON shipment_items(shipment_id);
CREATE INDEX idx_shipping_events_shipment_id ON shipping_events(shipment_id);
CREATE INDEX idx_shipping_addresses_order_id ON shipping_addresses(order_id);
CREATE INDEX idx_processed_events_order_id ON processed_events(order_id);

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
EOSQL

# ===================== ANALYTICS DB =====================
echo ">>> Initializing analytics_db..."
psql -v ON_ERROR_STOP=1 --username "$PGUSER" --dbname="analytics_db" <<'EOSQL'
CREATE TABLE order_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_hour TIMESTAMP NOT NULL,
    total_orders INT DEFAULT 0,
    confirmed_orders INT DEFAULT 0,
    shipped_orders INT DEFAULT 0,
    delivered_orders INT DEFAULT 0,
    cancelled_orders INT DEFAULT 0,
    total_revenue NUMERIC(19, 2) DEFAULT 0,
    avg_order_value NUMERIC(19, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_metric_hour UNIQUE(metric_hour)
);

CREATE TABLE revenue_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    revenue_date DATE NOT NULL UNIQUE,
    total_revenue NUMERIC(19, 2) NOT NULL,
    total_orders INT NOT NULL,
    avg_order_value NUMERIC(19, 2),
    payment_success_count INT DEFAULT 0,
    payment_failure_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE failure_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    failure_reason VARCHAR(255),
    order_id UUID,
    affected_service VARCHAR(50),
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved BOOLEAN DEFAULT FALSE
);

CREATE TABLE customer_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL UNIQUE,
    total_orders INT DEFAULT 0,
    total_spent NUMERIC(19, 2) DEFAULT 0,
    avg_order_value NUMERIC(19, 2) DEFAULT 0,
    last_order_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_performance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    total_units_sold INT DEFAULT 0,
    total_revenue NUMERIC(19, 2) DEFAULT 0,
    avg_rating NUMERIC(3, 2),
    rank_by_sales INT,
    rank_by_revenue INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_product_perf UNIQUE(product_id)
);

CREATE TABLE payment_failures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    failure_reason VARCHAR(255) NOT NULL,
    failure_count INT DEFAULT 1,
    percentage_of_total NUMERIC(5, 2),
    first_occurred TIMESTAMP,
    last_occurred TIMESTAMP,
    resolved_count INT DEFAULT 0
);

CREATE TABLE kafka_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consumer_group VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    lag BIGINT,
    consumer_offset BIGINT,
    log_end_offset BIGINT,
    measured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_kafka_metric UNIQUE(consumer_group, topic, measured_at)
);

CREATE INDEX idx_order_metrics_metric_hour ON order_metrics(metric_hour);
CREATE INDEX idx_revenue_records_revenue_date ON revenue_records(revenue_date);
CREATE INDEX idx_failure_events_event_type ON failure_events(event_type);
CREATE INDEX idx_failure_events_occurred_at ON failure_events(occurred_at);
CREATE INDEX idx_customer_metrics_customer_id ON customer_metrics(customer_id);
CREATE INDEX idx_product_performance_product_id ON product_performance(product_id);
CREATE INDEX idx_product_performance_rank ON product_performance(rank_by_sales, rank_by_revenue);
CREATE INDEX idx_payment_failures_reason ON payment_failures(failure_reason);
CREATE INDEX idx_kafka_metrics_consumer_group ON kafka_metrics(consumer_group);
CREATE INDEX idx_kafka_metrics_measured_at ON kafka_metrics(measured_at);

INSERT INTO order_metrics (metric_hour)
SELECT DATE_TRUNC('hour', NOW()) AS metric_hour
WHERE NOT EXISTS (SELECT 1 FROM order_metrics WHERE metric_hour = DATE_TRUNC('hour', NOW()));

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
EOSQL

echo ">>> All OrderFlow databases initialized successfully."
