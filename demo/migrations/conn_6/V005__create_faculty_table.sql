-- Migration: create_faculty_table
-- Version: 005

-- Write your UP SQL here
create table faculty(
    fac_id int primary key ,
    fac_name varchar(132)
);

-- DOWN

-- Write your DOWN SQL here
DELETE TABLE faculty;