-- Migration: insert_into_fasio
-- Version: 006

-- Write your UP SQL here
INSERT INTO fasio (id, name)
VALUES (
    1,
    'Fasio'
);

-- DOWN

-- Write your DOWN SQL here
DELETE FROM fasio
WHERE id = 1;