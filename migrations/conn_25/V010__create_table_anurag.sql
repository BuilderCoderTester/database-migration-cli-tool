-- Migration: create_table_anurag
-- Version: 010

-- Write your UP SQL here
CREATE TABLE anurag(
    id INT PRIMARY KEY , 
    name VARCHAR(123)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE anurag;