-- Migration: create_table_animal
-- Version: 001

-- Write your UP SQL here
CREATE TABLE animal(
    id INT PRIMARY KEY , 
    name VARCHAR(123),
    roll INT,
    address VARCHAR(123),
    birth VARCHAR(123)
); 

-- DOWN

-- Write your DOWN SQL here
DROP TABLE animal;