package com.project.demo.infrastructure.exception;

import com.project.demo.modules.migration.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex,
            HttpServletRequest request) {

        log.error("{} on {} {}",
                ex.getErrorCode(),
                request.getMethod(),
                request.getRequestURI(),
                ex);

        return build(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode(),
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            Exception ex,
            HttpServletRequest request) {

        log.warn("Bad request on {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        return build(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseError(
            DataAccessException ex,
            HttpServletRequest request) {

        log.error("Database error on {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "DATABASE_ERROR",
                "Database operation failed.",
                request
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIoError(
            IOException ex,
            HttpServletRequest request) {

        log.error("File operation failed on {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "FILE_ERROR",
                "File operation failed.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error on {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.",
                request
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request) {

        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(
                        errorCode,
                        message,
                        request.getRequestURI()
                ));
    }
}