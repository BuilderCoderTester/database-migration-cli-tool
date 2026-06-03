-- Migration: insert_into_criteria
-- Version: 005

-- Write your UP SQL here
INSERT INTO criteria
VALUES (1,'facial',123);

-- DOWN

-- Write your DOWN SQL here
delete from criteria where id = 1;