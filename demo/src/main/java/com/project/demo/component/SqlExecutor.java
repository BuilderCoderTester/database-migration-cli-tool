package com.project.demo.component;

import com.project.demo.service.CommonService;
import com.project.demo.utility.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SqlExecutor {
    private final JdbcTemplate jdbcTemplate;
    private final CommonService helper;


    public void executeScript(String script) {
        for (String statement : helper.splitSql(script)) {
            System.out.println("THE STATEMENT : "+ statement);
            if (!statement.isBlank()) {
                jdbcTemplate.execute(statement);
            }
        }
    }
}