-- Migration: update_users
-- Version: 13

-- Write your UP SQL here
UPDATE users
SET phone='9999999999'
WHERE username='John';

-- DOWN

-- Write your DOWN SQL here

UPDATE users
SET phone=NULL
WHERE username='John';