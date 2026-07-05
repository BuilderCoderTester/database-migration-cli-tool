package com.project.demo.migrationValidator.interfaces;

import com.project.demo.migrationValidator.exception.ValidationException;
import com.project.demo.modules.migration.model.MigrationScript;

import java.sql.Connection;

public interface MigrationValidator {
    void validate(MigrationScript script, Connection connection) throws ValidationException;

}
