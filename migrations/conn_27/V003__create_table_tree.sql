-- Migration: create_table_tree
-- Version: 003

-- Write your UP SQL here
CREATE TABLE tree(
    id INT PRIMARY KEY ,
    name VARCHAR(123),
    status VARCHAR(123),
    number INT
); 

-- DOWN

-- Write your DOWN SQL here
DROP TABLE tree;