package com.project.demo.exception;

public class MigrationCreationException extends RuntimeException {

    public MigrationCreationException(String message) {
        super(message);
    }

    public MigrationCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}