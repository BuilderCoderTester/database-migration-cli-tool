-- Migration: create_table_users
-- Version: 3

-- Write your UP SQL here

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(200) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- DOWN

-- Write your DOWN SQL here
DROP TABLE users;