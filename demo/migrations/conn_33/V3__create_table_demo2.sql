-- Migration: create_table_demo2
-- Version: 3

-- Write your UP SQL here
CREATE TABLE demo2
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- DOWN

-- Write your DOWN SQL here

DROP TABLE demo2;