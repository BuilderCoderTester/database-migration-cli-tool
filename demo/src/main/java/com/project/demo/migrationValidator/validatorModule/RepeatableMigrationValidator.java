package com.project.demo.migrationValidator.validatorModule;

import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.migrationValidator.interfaces.MigrationValidator;
import com.project.demo.modules.migration.model.MigrationScript;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class RepeatableMigrationValidator implements MigrationValidator {
    @Override
    public void validate(MigrationScript script, Connection connection) throws ValidationException {

        if (!script.isRepeatable()) {

            String downScript = script.getDownScript();

            if (downScript == null) {
                throw new ValidationException(
                        "Down script is required for versioned migrations.");
            }

            String actualScript = downScript
                    .replace("-- Write your DOWN SQL here", "")
                    .trim();

            if (actualScript.isEmpty()) {
                throw new ValidationException(
                        "Down script is required for versioned migrations.");
            }
        }
    }
}
