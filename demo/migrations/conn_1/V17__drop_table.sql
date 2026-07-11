-- Migration: drop_table
-- Version: 17

-- Write your UP SQL here
DROP TABLE order_items;

-- DOWN

-- Write your DOWN SQL here
CREATE TABLE order_items(
    order_id INT,
    product_id INT,
    quantity INT
);