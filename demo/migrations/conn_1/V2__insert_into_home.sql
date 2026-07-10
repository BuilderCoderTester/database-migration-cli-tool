-- Migration: insert_into_home
-- Version: 2

-- Write your UP SQL here
INSERT INTO home VALUES(1,'ANURAG');

-- DOWN

-- Write your DOWN SQL here

DELETE FROM home WHERE id = 1;