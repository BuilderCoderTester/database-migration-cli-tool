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


    public void executeScript(String script, Connection connection,String currentDatabase) throws SQLException, SQLException {

        System.out.println("The database in executeScript = "+ currentDatabase);
        System.out.println("the script in execute script is "+ script);
        for (String statement : helper.splitSql(script)) {

            System.out.println("here 2"  +statement);

            if (!statement.isBlank()) {

                System.out.println("Executing: " + statement);

                try (PreparedStatement preparedStatement =
                             connection.prepareStatement(statement)) {

                    preparedStatement.execute();
                }
                System.out.println("e bhai kaj korhcce");
            }
        }
    }

}