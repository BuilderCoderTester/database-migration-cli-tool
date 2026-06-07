-- Migration: create_table_master
-- Version: 006

-- Write your UP SQL here
create table master(masterId int primary key ,master varchar(124));

-- DOWN

-- Write your DOWN SQL here
DELETE TABLE master;