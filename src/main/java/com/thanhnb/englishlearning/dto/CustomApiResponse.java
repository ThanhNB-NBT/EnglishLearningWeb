package com.thanhnb.englishlearning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard API response wrapper")
public class CustomApiResponse<T> {
    @Schema(description = "HTTP status code", example = "200")
    private int status;

    @Schema(description = "Response message", example = "Success")
    private String message;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Timestamp of the response", example = "2025-09-24T10:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "Whether the request was successful", example = "true")
    private boolean success;

    // Static methods để tạo response dễ dàng
    public static <T> CustomApiResponse<T> success(T data) {
        return new CustomApiResponse<>(
            200, 
            "Success", 
            data, 
            LocalDateTime.now(), 
            true
        );
    }

    public static <T> CustomApiResponse<T> success(T data, String message) {
        return new CustomApiResponse<>(
            200, 
            message, 
            data, 
            LocalDateTime.now(), 
            true
        );
    }

    public static <T> CustomApiResponse<T> created(T data, String message) {
        return new CustomApiResponse<>(
            201, 
            message, 
            data, 
            LocalDateTime.now(), 
            true
        );
    }

    public static <T> CustomApiResponse<T> error(int status, String message) {
        return new CustomApiResponse<>(
            status, 
            message, 
            null, 
            LocalDateTime.now(), 
            false
        );
    }

    public static <T> CustomApiResponse<T> notFound(String message) {
        return new CustomApiResponse<>(
            404, 
            message, 
            null, 
            LocalDateTime.now(), 
            false
        );
    }

    public static <T> CustomApiResponse<T> badRequest(String message) {
        return new CustomApiResponse<>(
            400, 
            message, 
            null, 
            LocalDateTime.now(), 
            false
        );
    }
}