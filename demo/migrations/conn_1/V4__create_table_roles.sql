-- Migration: create_table_roles
-- Version: 4

-- Write your UP SQL here

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    role_name VARCHAR(100) UNIQUE
);

-- DOWN

-- Write your DOWN SQL here

DROP TABLE roles;