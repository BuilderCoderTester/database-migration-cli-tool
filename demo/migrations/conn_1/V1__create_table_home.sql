-- Migration: create_table_home
-- Version: 1

-- Write your UP SQL here

CREATE TABLE home(
id INT PRIMARY KEY ,
name VARCHAR(1243)
);

-- DOWN

-- Write your DOWN SQL here

DROP TABLE home;