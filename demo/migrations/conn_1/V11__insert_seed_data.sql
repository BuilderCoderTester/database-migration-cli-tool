-- Migration: insert_seed_data
-- Version: 11

-- Write your UP SQL here
INSERT INTO roles(role_name)
VALUES
('ADMIN'),
('USER');


-- DOWN

-- Write your DOWN SQL here


DELETE FROM roles;