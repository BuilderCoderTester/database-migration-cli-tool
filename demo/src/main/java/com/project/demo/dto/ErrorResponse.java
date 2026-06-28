package com.project.demo.dto;

import java.time.Instant;

public record ErrorResponse(
        boolean success,
        String errorCode,
        String message,
        String path,
        Instant timestamp
) {
    public static ErrorResponse of(String errorCode, String message, String path) {
        return new ErrorResponse(false, errorCode, message, path, Instant.now());
    }
}
