package com.project.demo.migrationValidator.validatorModule;

import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.migrationValidator.interfaces.MigrationValidator;
import com.project.demo.modules.migration.model.MigrationScript;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class DangerousSqlValidator implements MigrationValidator {
    @Override
    public void validate(MigrationScript script, Connection connection) throws ValidationException {

        String upScript = script.getUpScript();

        if (upScript == null) {
            return;
        }

        String[] lines = upScript.split("\\R");

        StringBuilder actualScript = new StringBuilder();

        // Skip the template header
        for (int i = 4; i < lines.length; i++) {
            actualScript.append(lines[i]).append("\n");
        }

        String sql = actualScript.toString().toUpperCase();

        if (sql.contains("DROP DATABASE")) {
            throw new ValidationException(
                    "DROP DATABASE statement is not allowed."
            );
        }
    }
}
