package com.thanhnb.englishlearning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private boolean success;

    // Static methods để tạo response dễ dàng
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            200, 
            "Success", 
            data, 
            LocalDateTime.now(), 
            true
        );
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
            200, 
            message, 
            data, 
            LocalDateTime.now(), 
            true
        );
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(
            201, 
            message, 
            data, 
            LocalDateTime.now(), 
            true
        );
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(
            status, 
            message, 
            null, 
            LocalDateTime.now(), 
            false
        );
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(
            404, 
            message, 
            null, 
            LocalDateTime.now(), 
            false
        );
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(
            400, 
            message, 
            null, 
            LocalDateTime.now(), 
            false
        );
    }
}
