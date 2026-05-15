package com.project.demo.component;

import com.project.demo.service.CommonService;
import com.project.demo.utility.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class SqlExecutor {
    private final JdbcTemplate jdbcTemplate;
    private final CommonService helper;


    public void executeScript(String script, Connection connection) throws SQLException, SQLException {

        System.out.println("here 1");

        for (String statement : helper.splitSql(script)) {

            System.out.println("here 2");

            if (!statement.isBlank()) {

                System.out.println("Executing: " + statement);

                try (PreparedStatement preparedStatement =
                             connection.prepareStatement(statement)) {

                    preparedStatement.execute();
                }
            }
        }
    }

}