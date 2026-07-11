-- Migration: add_phone_to_users
-- Version: 6

-- Write your UP SQL here
ALTER TABLE users
ADD COLUMN phone VARCHAR(20);


-- DOWN

-- Write your DOWN SQL here


ALTER TABLE users
DROP COLUMN phone;