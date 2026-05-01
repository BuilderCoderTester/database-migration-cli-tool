-- Migration: create_table_Gender
-- Version: 007

-- Write your UP SQL here
create table Gender(
    id int primary key,
    gender varchar(120)
);

-- DOWN

-- Write your DOWN SQL here
drop table Gender;