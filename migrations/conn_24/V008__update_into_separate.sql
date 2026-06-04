-- Migration: update_into_separate
-- Version: 008

-- Write your UP SQL here
UPDATE separate
SET bf = 'Anurag'
WHERE bf = 'OldName';

-- DOWN

-- Write your DOWN SQL here
UPDATE separate
SET bf = 'OldName'
WHERE bf = 'Anurag';