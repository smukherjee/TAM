package com.utam.tracking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Global exception handler for tracking controllers.
 * Feature: 005-asset-tracking-security
 * Task: T063
 */
@RestControllerAdvice(basePackages = "com.utam.tracking.controller")
public class TrackingExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TrackingExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception in tracking controller: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "Resource not found",
                "message", ex.getMessage(),
                "timestamp", ZonedDateTime.now().toString()
            ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Validation error in tracking controller: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "Validation error",
                "message", ex.getMessage(),
                "timestamp", ZonedDateTime.now().toString()
            ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied in tracking controller: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "error", "Access denied",
                "message", "You do not have permission to access this resource",
                "timestamp", ZonedDateTime.now().toString()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error in tracking controller: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "Internal server error",
                "message", "An unexpected error occurred",
                "timestamp", ZonedDateTime.now().toString()
            ));
    }
}
