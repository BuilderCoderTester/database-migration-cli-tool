-- Migration: drop_column
-- Version: 16

-- Write your UP SQL here

ALTER TABLE users
DROP COLUMN phone;

-- DOWN

-- Write your DOWN SQL here

ALTER TABLE users
ADD COLUMN phone VARCHAR(20);