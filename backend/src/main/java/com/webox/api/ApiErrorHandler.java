package com.webox.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {
    public record FieldError(String field, String message) {}
    public record ErrorBody(String code, String message, String correlationId, List<FieldError> fieldErrors) {}

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> handleApi(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus()).body(new ErrorBody(
                exception.getCode(), exception.getMessage(), request.getRequestId(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorBody(
                "VALIDATION_ERROR", "Please correct the highlighted fields.", request.getRequestId(), fields));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorBody> handleConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorBody(
                "DATA_CONFLICT", "This request conflicts with an existing record.", request.getRequestId(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(new ErrorBody(
                "INTERNAL_ERROR", "Something went wrong. Please try again.", request.getRequestId(), List.of()));
    }
}
