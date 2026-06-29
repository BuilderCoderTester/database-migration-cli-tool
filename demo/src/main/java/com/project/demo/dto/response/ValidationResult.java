package com.project.demo.dto.response;

import lombok.Data;

@Data
public class ValidationResult {

    private boolean valid;

    private String message;

    private String existingMigrationVersion;

    private String existingMigrationName;

    private String tableName;

    public static ValidationResult error(String s) {
        return  null;
    }

    public static ValidationResult success(String s) {
        return null;
    }
}
