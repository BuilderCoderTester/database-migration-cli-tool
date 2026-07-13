-- Migration: create_table_department
-- Version: 6

-- Write your UP SQL here

CREATE TABLE department(
id INT PRIMARY KEY , 
name VARCHAR(1233),
roll INT
);

-- DOWN

-- Write your DOWN SQL here

DROP TABLE department;