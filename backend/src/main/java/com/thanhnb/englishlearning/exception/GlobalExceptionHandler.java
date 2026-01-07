package com.thanhnb.englishlearning.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ Handle validation errors với chi tiết
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", errors);

        System.err.println("❌ Validation errors: " + errors);

        return ResponseEntity.badRequest().body(response);
    }

    // ✅ Handle HandlerMethodValidationException (Spring 6.x)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        // ✅ Log toàn bộ exception details
        System.err.println("❌ VALIDATION ERROR DETAILS:");
        System.err.println("❌ Exception message: " + ex.getMessage());
        System.err.println("❌ Value results count: " + ex.getValueResults().size());

        ex.getValueResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            System.err.println("❌ Parameter: " + paramName);

            result.getResolvableErrors().forEach(error -> {
                String errorMsg = error.getDefaultMessage();
                System.err.println("❌   - Error: " + errorMsg);
                errors.put(paramName, errorMsg);
            });
        });

        // ✅ Also check for nested validation errors
        if (ex.getAllErrors() != null) {
            ex.getAllErrors().forEach(error -> {
                System.err.println("❌ Global error: " + error);
            });
        }

        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", errors);
        response.put("detailedMessage", ex.getMessage()); // ✅ Add full message

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex) {
        ex.printStackTrace(); // Log full stack trace

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}