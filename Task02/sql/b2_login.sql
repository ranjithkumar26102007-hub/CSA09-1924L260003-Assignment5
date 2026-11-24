-- =============================================================================
-- Course: CSA09 – Programming in Java
-- Assignment 5 (CO5) - Task B2: Adversarial Input Analysis Schema
-- Student: R. Ranjith Kumar | Register Number: 1924L260003
-- Purpose: Schema setup for demonstrating SQL Injection in unsafe Statement
--          versus immunization using PreparedStatement.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS csa09_assignment5;
USE csa09_assignment5;

DROP TABLE IF EXISTS app_user;

CREATE TABLE app_user (
    username VARCHAR(50) PRIMARY KEY,
    role VARCHAR(20) NOT NULL,
    password VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed test users
INSERT INTO app_user (username, role, password) VALUES
    ('admin', 'ADMIN', 'admin123'),
    ('vignesh', 'STUDENT', 'pass123'),
    ('ranjith', 'STUDENT', 'secret456');

-- Verification
SELECT * FROM app_user;
