-- Migration: create_table_memo
-- Version: 8

-- Write your UP SQL here

CREATE TABLE memo(
	id INT PRIMARY KEY,
name VARCHAR(12)
);

-- DOWN

-- Write your DOWN SQL here

DROP TABLE memo;