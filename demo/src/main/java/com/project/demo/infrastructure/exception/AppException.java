package com.project.demo.infrastructure.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException{
    private final String errorCode;

    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
