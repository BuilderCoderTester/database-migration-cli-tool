-- Migration: create_table_fasio
-- Version: 007

-- Write your UP SQL here
CREATE TABLE fasio (
    id INT PRIMARY KEY,
    name VARCHAR(255)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE fasio;