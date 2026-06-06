-- Migration: insert_into_iron
-- Version: 004

-- Write your UP SQL here
CREATE TABLE iron
(id INT PRIMARY KEY ,
name VARCHAR(123)
 );

-- DOWN

-- Write your DOWN SQL here
DROP TABLE iron;