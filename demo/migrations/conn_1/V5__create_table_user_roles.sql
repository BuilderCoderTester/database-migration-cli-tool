-- Migration: create_table_user_roles
-- Version: 5

-- Write your UP SQL here
CREATE TABLE user_roles (
    user_id INT,
    role_id INT,

    PRIMARY KEY(user_id, role_id),

    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id),

    CONSTRAINT fk_role
        FOREIGN KEY(role_id)
        REFERENCES roles(id)
);


-- DOWN

-- Write your DOWN SQL here


DROP TABLE user_roles;