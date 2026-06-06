-- Migration: insert_into_man
-- Version: 003

-- Write your UP SQL here
INSERT INTO man (id, name)
VALUES (1, 'Anurag');

-- DOWN

-- Write your DOWN SQL here
DELETE FROM man
WHERE id = 1;