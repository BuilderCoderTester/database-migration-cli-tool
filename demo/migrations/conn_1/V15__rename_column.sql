-- Migration: rename_column
-- Version: 15

-- Write your UP SQL here

ALTER TABLE users
RENAME COLUMN username TO full_name;


-- DOWN

-- Write your DOWN SQL here


ALTER TABLE users
RENAME COLUMN full_name TO username;