-- =============================================================================
-- Course: CSA09 – Programming in Java
-- Assignment 5 (CO5) - Task B4: Enterprise Migration Schema
-- Student: R. Ranjith Kumar | Register Number: 1924L260003
-- Purpose: Schema setup for enterprise migration engine supporting 12 campuses
-- =============================================================================

CREATE DATABASE IF NOT EXISTS csa09_assignment5;
USE csa09_assignment5;

DROP TABLE IF EXISTS enterprise_bus_pass;

CREATE TABLE enterprise_bus_pass (
    pass_id VARCHAR(16) PRIMARY KEY,
    campus_id VARCHAR(10) NOT NULL DEFAULT 'CAMPUS_01',
    register_no VARCHAR(16) NOT NULL,
    name VARCHAR(80) NOT NULL,
    route_no VARCHAR(8) NOT NULL,
    boarding_stop VARCHAR(60) NOT NULL,
    valid_until DATE NOT NULL,
    amount_paise BIGINT NOT NULL,
    CONSTRAINT chk_ent_amount CHECK (amount_paise > 0 AND amount_paise <= 5000000) -- max Rs 50,000 ceiling
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DESCRIBE enterprise_bus_pass;
