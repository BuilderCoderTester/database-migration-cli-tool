package com.project.demo.migrationValidator.validatorModule;

import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.migrationValidator.interfaces.MigrationValidator;
import com.project.demo.modules.migration.model.MigrationScript;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
public class MigrationExistsValidator implements MigrationValidator {
    @Override
    public void validate(MigrationScript script, Connection connection) throws ValidationException {
        if (script == null) {
            throw new ValidationException("Migration not found.");
        }
    }
}
