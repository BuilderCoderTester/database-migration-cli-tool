-- Migration: insert_into_tree
-- Version: 002

-- Write your UP SQL here
INSERT INTO tree
    VALUES (
    1,'Cactuss','never ending',123
);

-- DOWN

-- Write your DOWN SQL here
DELETE FROM tree
WHERE id = 1;