package com.project.demo.component;

import com.project.demo.service.CommonService;
import com.project.demo.utility.Helper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
@Slf4j
public class SqlExecutor {
    private final JdbcTemplate jdbcTemplate;
    private final CommonService helper;


    public void executeScript(String script, Connection connection,String currentDatabase) throws SQLException, SQLException {

        log.debug("Executing migration script against database {}", currentDatabase);
        for (String statement : helper.splitSql(script)) {

            log.trace("Prepared SQL statement: {}", statement);

            if (!statement.isBlank()) {

                log.debug("Executing SQL statement");

                try (PreparedStatement preparedStatement =
                             connection.prepareStatement(statement)) {

                    preparedStatement.execute();
                }
                log.debug("SQL statement executed successfully");
            }
        }
    }

}
