-- OrderFlow: Analytics Database Initialization

CREATE DATABASE analytics_db;

\c analytics_db;

-- Order metrics (aggregated by hour)
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

-- Revenue records (daily)
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

-- Failure events
CREATE TABLE failure_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    failure_reason VARCHAR(255),
    order_id UUID,
    affected_service VARCHAR(50),
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved BOOLEAN DEFAULT FALSE
);

-- Customer metrics
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

-- Product performance
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

-- Payment failure analysis
CREATE TABLE payment_failures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    failure_reason VARCHAR(255) NOT NULL,
    failure_count INT DEFAULT 1,
    percentage_of_total NUMERIC(5, 2),
    first_occurred TIMESTAMP,
    last_occurred TIMESTAMP,
    resolved_count INT DEFAULT 0
);

-- Kafka consumer lag metrics
CREATE TABLE kafka_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consumer_group VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    lag BIGINT,
    offset BIGINT,
    log_end_offset BIGINT,
    measured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_kafka_metric UNIQUE(consumer_group, topic, measured_at)
);

-- Indexes
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

-- Initialize hourly metric template
INSERT INTO order_metrics (metric_hour)
SELECT DATE_TRUNC('hour', NOW()) AS metric_hour
WHERE NOT EXISTS (SELECT 1 FROM order_metrics WHERE metric_hour = DATE_TRUNC('hour', NOW()));

GRANT ALL PRIVILEGES ON DATABASE analytics_db TO postgres;
