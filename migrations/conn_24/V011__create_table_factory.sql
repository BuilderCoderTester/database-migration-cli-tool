-- Migration: create_table_factory
-- Version: 011

-- Write your UP SQL here
CREATE TABLE factory(
    id INT PRIMARY KEY,
    name VARCHAR(12)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE factory;