-- Migration: create users table
-- Version: 001"public"

CREATE TABLE users (id INT PRIMARY KEY ,
                    first_name varchar(120) ,
                    last_name varchar(120))