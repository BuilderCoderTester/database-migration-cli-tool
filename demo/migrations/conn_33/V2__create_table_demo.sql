-- Migration: create_table_demo
-- Version: 2

-- Write your UP SQL here
CREATE TABLE demo (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    age INT,
    city VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE IF EXISTS demo;