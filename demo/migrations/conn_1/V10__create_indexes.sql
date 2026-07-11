-- Migration: create_indexes
-- Version: 10

-- Write your UP SQL here
CREATE INDEX idx_users_email
ON users(email);

-- DOWN

-- Write your DOWN SQL here


DROP INDEX idx_users_email;