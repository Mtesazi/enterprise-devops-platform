package com.mtesazi.employeeservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import com.mtesazi.employeeservice.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(
            EmployeeNotFoundException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DepartmentReferenceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentReferenceNotFound(
            DepartmentReferenceNotFoundException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DepartmentServiceTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentServiceTimeout(
            DepartmentServiceTimeoutException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DepartmentServiceCommunicationException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentServiceCommunication(
            DepartmentServiceCommunicationException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input validation failed",
                errors
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String expectedType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "valid value";
        String message = "Invalid value '" + ex.getValue() + "' for '" + ex.getName()
                + "'. Expected " + expectedType + ".";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getRequestURI(),
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> errors) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase() + " at " + path,
                message,
                errors
        );
        return ResponseEntity.status(status).body(response);
    }
}
