-- Migration: create_table_persona
-- Version: 008

-- Write your UP SQL here
CREATE TABLE persona(
    id INT PRIMARY KEY,
    name VARCHAR(120)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE persona;