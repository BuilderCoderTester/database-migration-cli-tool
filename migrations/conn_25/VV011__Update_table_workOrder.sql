-- Migration: Update table workOrder
-- Version: V011

-- Migration: create_table_workOrder
-- Version: 011

-- Write your UP SQL here
CREATE TABLE workOrder(
    id INT PRIMARY KEY,
    name VARCHAR(123),
    role VARCHAR(123),
    role_name VARCHAR(123)
);

-- DOWN

-- Write your DOWN SQL here
DROP TABLE workOrder;