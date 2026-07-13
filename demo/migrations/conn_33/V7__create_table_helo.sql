-- Migration: create_table_helo
-- Version: 7

-- Write your UP SQL here

CREATE TABLE helo(
id INT PRIMARY KEY,
name VARCHAR(12)
);

-- DOWN

-- Write your DOWN SQL here

DROP TABLE helo;