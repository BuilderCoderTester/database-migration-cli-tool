-- Migration: create_table_products
-- Version: 8

-- Write your UP SQL here
CREATE TABLE products(
    id SERIAL PRIMARY KEY,
    name VARCHAR(200),
    price DECIMAL(10,2)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE products;