-- =====================================================
-- Customer Service - TEST Database Schema (PostgreSQL)
-- =====================================================

-- Drop tables if exist (for clean test execution)
DROP TABLE IF EXISTS customer CASCADE;
DROP TABLE IF EXISTS person CASCADE;

-- Table: person (base entity)
CREATE TABLE person (
    person_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    age INTEGER NOT NULL,
    identification VARCHAR(50) NOT NULL UNIQUE,
    address VARCHAR(500),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL
);

-- Table: customer (extends person)
CREATE TABLE customer (
    customer_id UUID PRIMARY KEY,
    person_id UUID NOT NULL REFERENCES person(person_id) ON DELETE CASCADE,
    password VARCHAR(255) NOT NULL,
    state BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

-- Indexes for performance
CREATE INDEX idx_person_identification ON person(identification);
CREATE INDEX idx_customer_person_id ON customer(person_id);
CREATE INDEX idx_customer_state ON customer(state);
