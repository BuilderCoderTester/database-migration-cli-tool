package com.project.demo.infrastructure.exception;

public class MigrationValidationException extends RuntimeException {
    public MigrationValidationException(String message) {
        super(message);
    }
}
