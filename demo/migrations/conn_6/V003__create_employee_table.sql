-- Migration: create_employee_table
-- Version: 003

-- Write your UP SQL here
CREATE TABLE employee (
    emp_id int PRIMARY KEY ,
    emp_name VARCHAR(120)
)

-- DOWN

-- Write your DOWN SQL here
DELETE table employee;