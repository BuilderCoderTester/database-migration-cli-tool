-- Migration: insert_into_separate
-- Version: 010

-- Write your UP SQL here
INSERT INTO separate value(1,'Anurag');

-- DOWN

-- Write your DOWN SQL here
DELETE FROM separate WHERE id = 1;