-- Migration: create_table_resina
-- Version: 007

-- Write your UP SQL here
CREATE TABLE resina(
    id INT PRIMARY KEY,
    name VARCHAR(12)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE resina;