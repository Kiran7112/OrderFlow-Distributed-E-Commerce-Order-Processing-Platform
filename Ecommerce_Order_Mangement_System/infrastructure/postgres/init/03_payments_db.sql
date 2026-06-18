-- OrderFlow: Payments Database Initialization

CREATE DATABASE payments_db;

\c payments_db;

-- Transactions table
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

-- Payment retries table
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

-- Refunds table
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

-- Processed events table (idempotency)
CREATE TABLE processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    order_id UUID NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_transactions_order_id ON transactions(order_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_payment_retries_transaction_id ON payment_retries(transaction_id);
CREATE INDEX idx_refunds_transaction_id ON refunds(transaction_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_processed_events_order_id ON processed_events(order_id);

GRANT ALL PRIVILEGES ON DATABASE payments_db TO postgres;
