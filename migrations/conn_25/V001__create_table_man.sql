-- Migration: create_table_man
-- Version: 001

-- Write your UP SQL here
CREATE TABLE man(
    id INT PRIMARY KEY,
    name VARCHAR(123)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE man;