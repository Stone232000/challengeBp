-- =====================================================
-- Account Service - TEST Database Schema (PostgreSQL)
-- =====================================================

-- Drop tables if exist (for clean test execution)
DROP TABLE IF EXISTS movement CASCADE;
DROP TABLE IF EXISTS account CASCADE;

-- Table: account
CREATE TABLE account (
    account_number BIGSERIAL PRIMARY KEY,
    customer_id UUID NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    initial_balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    state BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    version INTEGER NOT NULL DEFAULT 1
);

-- Table: movement
CREATE TABLE movement (
    movement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number BIGINT NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    balance_before DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    description VARCHAR(500),
    reference VARCHAR(255),
    transaction_id VARCHAR(255) NOT NULL,
    reversed_movement_id UUID DEFAULT NULL,
    reversed BOOLEAN NOT NULL DEFAULT FALSE,
    idempotency_key UUID DEFAULT NULL,
    correlation_id UUID DEFAULT NULL,
    request_id UUID DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_movement_transaction_id UNIQUE (transaction_id),
    CONSTRAINT uk_movement_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_movement_account FOREIGN KEY (account_number) REFERENCES account(account_number) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_account_customer_id ON account(customer_id);
CREATE INDEX idx_account_type ON account(account_type);
CREATE INDEX idx_movement_account_number ON movement(account_number);
CREATE INDEX idx_movement_created_at ON movement(created_at);
CREATE INDEX idx_movement_transaction_id ON movement(transaction_id);
