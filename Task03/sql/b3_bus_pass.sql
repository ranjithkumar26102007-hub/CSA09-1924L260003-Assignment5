-- =============================================================================
-- Course: CSA09 – Programming in Java
-- Assignment 5 (CO5) - Task B3: Bus Pass Relational Schema & Constraints
-- Student: R. Ranjith Kumar | Register Number: 1924L260003
-- Purpose: Schema setup for JDBC performance benchmarks, batch loading,
--          and transaction rollback validation under constraint failures.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS csa09_assignment5;
USE csa09_assignment5;

DROP TABLE IF EXISTS bus_pass;

CREATE TABLE bus_pass (
    pass_id VARCHAR(16) PRIMARY KEY,
    register_no VARCHAR(16) NOT NULL,
    name VARCHAR(80) NOT NULL,
    route_no VARCHAR(8) NOT NULL,
    boarding_stop VARCHAR(60) NOT NULL,
    valid_until DATE NOT NULL,
    amount_paise BIGINT NOT NULL,
    CONSTRAINT chk_amount CHECK (amount_paise >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verification
DESCRIBE bus_pass;
