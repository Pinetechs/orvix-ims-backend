package com.pinetechs.orvix.ims.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.ParseException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ApiResponse.failed(ex.getReason()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex) {

        System.err.println("AccessDeniedException: " + ex.getLocalizedMessage());
        List<String> errors = new ArrayList<>();
        errors.add("Access denied: " + ex.getMessage());
        String message = errors.isEmpty() ? "Access denied" : String.join(", ", errors);
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failed(message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {

        List<String> errors = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        });

        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
        });

        String message = errors.isEmpty() ? "Validation error" : String.join(", ", errors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failed(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleConstraintViolationException(ConstraintViolationException ex) {

        List<String> errors = new ArrayList<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }

        String message = errors.isEmpty() ? "Validation failed" : String.join(", ", errors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failed(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        System.err.println("IllegalArgumentException: " + ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failed("Invalid argument: " + ex.getMessage()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiResponse> handleDateTimeParseException(DateTimeParseException ex) {

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failed("Invalid date/time format"));
    }

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ApiResponse> handleParseException(ParseException ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failed("Invalid value format"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.failed(ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.failed("Uploaded file size exceeds the allowed limit"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse> handleMultipartException(MultipartException ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failed("Invalid multipart request"));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse> handleIOException(IOException ex) {
        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failed("File processing error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failed("An internal server error occurred"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failed("Invalid username or password"));
    }


}