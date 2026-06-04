-- Migration: create_table_separate
-- Version: 009

-- Write your UP SQL here
CREATE TABLE separate
(
    id INT PRIMARY KEY ,
    bf VARCHAR(123)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE separate;