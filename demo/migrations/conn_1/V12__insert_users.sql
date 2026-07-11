-- Migration: insert_users
-- Version: 12

-- Write your UP SQL here

INSERT INTO users(username,email)
VALUES
('John','john@test.com'),
('Alice','alice@test.com');


-- DOWN

-- Write your DOWN SQL here

DELETE FROM users;