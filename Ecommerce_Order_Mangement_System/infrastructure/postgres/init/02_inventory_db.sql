-- OrderFlow: Inventory Database Initialization

CREATE DATABASE inventory_db;

\c inventory_db;

-- Products table
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

-- Stock table (with optimistic locking)
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

-- Stock reservations (audit trail)
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

-- Processed events table (idempotency)
CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    order_id UUID NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_stock_product_id ON stock(product_id);
CREATE INDEX idx_stock_reservations_order_id ON stock_reservations(order_id);
CREATE INDEX idx_stock_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX idx_stock_reservations_status ON stock_reservations(reservation_status);
CREATE INDEX idx_processed_events_order_id ON processed_events(order_id);

-- Sample products
INSERT INTO products (name, description, price, category, sku) VALUES
    ('Laptop Pro', 'High-performance laptop with 16GB RAM', 1299.99, 'Electronics', 'LAPTOP-001'),
    ('Wireless Mouse', 'Ergonomic wireless mouse with USB receiver', 29.99, 'Accessories', 'MOUSE-001'),
    ('USB-C Cable', 'Premium USB-C charging cable 2m', 19.99, 'Accessories', 'CABLE-001'),
    ('Monitor 4K', '27-inch 4K Ultra HD monitor', 399.99, 'Electronics', 'MONITOR-001'),
    ('Mechanical Keyboard', 'RGB mechanical gaming keyboard', 149.99, 'Accessories', 'KEYBOARD-001');

-- Initialize stock for sample products
INSERT INTO stock (product_id, available_qty, reserved_qty, reorder_level)
SELECT id, 100, 0, 10 FROM products;

GRANT ALL PRIVILEGES ON DATABASE inventory_db TO postgres;
