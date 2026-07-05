package com.project.demo.infrastructure.exception;

public class MigrationException extends RuntimeException {
    public MigrationException(String message) {
        super(message);
    }
}
