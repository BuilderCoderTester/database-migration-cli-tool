package com.project.demo.infrastructure.exception;

public class MigrationExecutionException extends AppException {
    public MigrationExecutionException(String message) {
        super("MIGRATION_EXECUTION_ERROR", message);
    }

    public MigrationExecutionException(String message, Throwable cause) {
        super("MIGRATION_EXECUTION_ERROR", message, cause);
    }
}
