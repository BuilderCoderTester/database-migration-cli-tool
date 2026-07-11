-- Migration: create_table_orders
-- Version: 7

-- Write your UP SQL here

CREATE TABLE orders(
    id SERIAL PRIMARY KEY,
    user_id INT,
    amount DECIMAL(10,2),

    CONSTRAINT fk_orders_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
);

-- DOWN

-- Write your DOWN SQL here

DROP TABLE orders;