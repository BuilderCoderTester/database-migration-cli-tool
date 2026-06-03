-- Migration: create_table_criteria
-- Version: 006

-- Write your UP SQL here
CREATE TABLE criteria(
    id INT PRIMARY KEY,
    name VARCHAR(123),
    opt INT
); 

-- DOWN

-- Write your DOWN SQL here
DROP TABLE criteria;