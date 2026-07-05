package com.project.demo.infrastructure.exception;

public class MigrationLockException extends RuntimeException {
    public MigrationLockException(String message) {
        super(message);
    }
}
