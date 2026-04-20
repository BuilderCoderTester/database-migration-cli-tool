package com.project.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommonService {
    /// split the sql for migration validation
    public List<String> splitSql(String sql) {

        List<String> statements = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        boolean inString = false;

        for (char c : sql.toCharArray()) {

            if (c == '\'') {
                inString = !inString;
            }

            if (c == ';' && !inString) {
                statements.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            statements.add(current.toString().trim());
        }

        return statements;
    }

}
