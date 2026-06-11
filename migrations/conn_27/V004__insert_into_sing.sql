-- Migration: insert_into_sing
-- Version: 004

-- Write your UP SQL here
INSERT INTO sing
VALUES(1,'anurag',123);

-- DOWN

-- Write your DOWN SQL here
DELETE FROM sing
WHERE id = 1;