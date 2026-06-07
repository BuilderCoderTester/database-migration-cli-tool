-- Migration: create_user_table
-- Version: 002

-- Write your UP SQL here
CREATE TABLE users (id INT PRIMARY KEY ,
                    first_name varchar(120) ,
                    last_name varchar(120));

-- DOWN

-- Write your DOWN SQL here
DELETE TABLE users;