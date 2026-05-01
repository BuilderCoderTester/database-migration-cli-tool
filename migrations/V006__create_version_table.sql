-- Migration: create_version_table
-- Version: 006

-- Write your UP SQL here
create table Version(
    ver_id int primary key,
    ver_description varchar(120)
);

-- DOWN

-- Write your DOWN SQL here
drop table Version;