-- Migration: create_table_demo
-- Version: 1

-- Write your UP SQL here

CREATE TABLE demo(
 id INT PRIMARY KEY,
name VARCHAR(123)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE demo;