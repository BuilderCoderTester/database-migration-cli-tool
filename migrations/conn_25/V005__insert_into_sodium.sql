-- Migration: insert_into_sodium
-- Version: 005

-- Write your UP SQL here
INSERT INTO sodium
VALUES (
    1,
    'NA'
);

-- DOWN

-- Write your DOWN SQL here
DELETE FROM sodium
WHERE id = 1;