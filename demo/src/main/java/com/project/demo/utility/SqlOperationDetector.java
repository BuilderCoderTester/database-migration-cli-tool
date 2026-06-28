package com.project.demo.utility;

import com.project.demo.enumuration.DatabaseOperation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Slf4j
public class SqlOperationDetector {

    public DatabaseOperation detectOperation(String sql) {
        String cleaned = sql.lines()
                .map(String::trim)
                .filter(line ->
                        !line.isBlank() &&
                                !line.startsWith("--"))
                .findFirst()
                .orElse("")
                .toUpperCase();
        log.trace("Detecting SQL operation from first statement: {}", cleaned);

        if (cleaned.startsWith("CREATE")) {
            return DatabaseOperation.CREATE;
        }

        if (cleaned.startsWith("ALTER")) {
            return DatabaseOperation.ALTER;
        }

        if (cleaned.startsWith("DROP")) {
            return DatabaseOperation.DROP;
        }

        if (cleaned.startsWith("INSERT")) {
            return DatabaseOperation.INSERT;
        }

        if (cleaned.startsWith("UPDATE")) {
            return DatabaseOperation.UPDATE;
        }

        if (cleaned.startsWith("DELETE")) {
            return DatabaseOperation.DELETE;
        }

        return DatabaseOperation.UNKNOWN;
    }
}
