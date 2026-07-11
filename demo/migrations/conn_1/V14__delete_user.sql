-- Migration: delete_user
-- Version: 14

-- Write your UP SQL here

DELETE FROM users
WHERE username='Alice';

-- DOWN

-- Write your DOWN SQL here


INSERT INTO users(username,email)
VALUES('Alice','alice@test.com');