package com.project.demo.infrastructure.exception;

public class MigrationLoadException extends AppException {
    public MigrationLoadException(String message) {
        super("MIGRATION_LOAD_ERROR",message);
    }

    public MigrationLoadException(String message,Throwable cause){
        super("MIGRATION_LOAD_ERROR",message,cause);
    }
}
