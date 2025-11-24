package com.thanhnb.englishlearning.exception;

import com.thanhnb.englishlearning.dto.CustomApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý tất cả BaseException (custom exceptions)
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<CustomApiResponse<Object>> handleBaseException(BaseException ex) {
        log.error("BaseException: {} - Status: {}", ex.getMessage(), ex.getStatusCode());
        
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(CustomApiResponse.error(ex.getStatusCode(), ex.getMessage()));
    }

    /**
     * Xử lý validation errors từ @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation failed: {}", errors);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.error(400, "Dữ liệu không hợp lệ"));
    }

    /**
     * Xử lý Spring Security AccessDeniedException
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CustomApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CustomApiResponse.error(403, "Bạn không có quyền truy cập tài nguyên này"));
    }

    /**
     * Xử lý IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CustomApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.error(400, ex.getMessage()));
    }

    /**
     * Xử lý NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<CustomApiResponse<Object>> handleNullPointerException(NullPointerException ex) {
        log.error("Null pointer exception", ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error(500, "Lỗi hệ thống: Dữ liệu không hợp lệ"));
    }

    /**
     * Xử lý tất cả exceptions còn lại (fallback)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error(500, "Lỗi hệ thống. Vui lòng thử lại sau."));
    }
    
    /**
     * Xử lý RuntimeException (nếu chưa được catch bởi BaseException)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CustomApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error(500, ex.getMessage()));
    }
}