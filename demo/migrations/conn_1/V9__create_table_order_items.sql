-- Migration: create_table_order_items
-- Version: 9

-- Write your UP SQL here
CREATE TABLE order_items(
    order_id INT,
    product_id INT,
    quantity INT,

    PRIMARY KEY(order_id, product_id),

    FOREIGN KEY(order_id)
        REFERENCES orders(id),

    FOREIGN KEY(product_id)
        REFERENCES products(id)
);


-- DOWN

-- Write your DOWN SQL here
DROP TABLE order_items;