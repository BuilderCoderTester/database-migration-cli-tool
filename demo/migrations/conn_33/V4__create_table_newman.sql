-- Migration: create_table_newman
-- Version: 4

-- Write your UP SQL here
CREATE TABLE newman (
id INT PRIMARY KEY ,
name VARCHAR(123)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE newman;