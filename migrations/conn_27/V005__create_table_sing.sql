-- Migration: create_table_sing
-- Version: 005

-- Write your UP SQL here
CREATE TABLE sing(
    id INT PRIMARY KEY,
    name VARCHAR(123),
    number INT
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE sing;