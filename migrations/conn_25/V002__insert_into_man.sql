-- Migration: insert_into_man
-- Version: 002

-- Write your UP SQL here
INSERT INTO man VALUE(1,"Anurag");

-- DOWN

-- Write your DOWN SQL here
DELETE FROM man WHERE id = 1;