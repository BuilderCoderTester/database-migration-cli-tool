-- Migration: create_table_petition
-- Version: 004

-- Write your UP SQL here
create table petition(
    pet_id INT PRIMARY KEY ,
    pet_name varchar(12)
);

-- DOWN

-- Write your DOWN SQL here
drop table petition;