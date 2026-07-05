package com.project.demo.migrationValidator.validatorModule;

import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.migrationValidator.interfaces.MigrationValidator;
import com.project.demo.modules.migration.model.MigrationScript;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class ScriptValidator implements MigrationValidator {
    @Override
    public void validate(MigrationScript script, Connection connection) throws ValidationException {
        String upScript = script.getUpScript();

        if (upScript == null) {
            throw new ValidationException("UP script is empty.");
        }

        String[] lines = upScript.split("\\R");

        StringBuilder actualScript = new StringBuilder();

        // Skip the first 4 lines:
        // 1. -- Migration: ...
        // 2. -- Version: ...
        // 3. Blank line
        // 4. -- Write your UP SQL here
        for (int i = 4; i < lines.length; i++) {
            actualScript.append(lines[i]).append("\n");
        }

        if (actualScript.toString().trim().isEmpty()) {
            throw new ValidationException("UP script is empty.");
        }
    }
}
