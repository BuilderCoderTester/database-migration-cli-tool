package com.project.demo.dto.response;

import lombok.Data;

@Data
public class ValidationResult {

    private boolean valid;

    private String message;

    private String existingMigrationVersion;

    private String existingMigrationName;

    private String tableName;

    public static ValidationResult error(String message) {
        ValidationResult result = new ValidationResult();
        result.setValid(false);
        result.setMessage(message);
        return result;
    }

    public static ValidationResult success(String message) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        result.setMessage(message);
        return result;
    }

    public static ValidationResult error(
            String message,
            String migrationVersion,
            String migrationName,
            String tableName) {

        ValidationResult result = new ValidationResult();
        result.setValid(false);
        result.setMessage(message);
        result.setExistingMigrationVersion(migrationVersion);
        result.setExistingMigrationName(migrationName);
        result.setTableName(tableName);
        return result;
    }
}