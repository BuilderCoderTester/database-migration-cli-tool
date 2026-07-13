-- Migration: insert_into_newman
-- Version: 5

-- Write your UP SQL here

INSERT INTO newman
VALUES(
1,'Anurag');

-- DOWN

-- Write your DOWN SQL here
DELETE FROM newman
WHERE id = 1;