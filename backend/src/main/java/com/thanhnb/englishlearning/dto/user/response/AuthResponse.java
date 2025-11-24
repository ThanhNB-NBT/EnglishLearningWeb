package com.thanhnb.englishlearning.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "JWT token for authentication")
    private String token;

    @Schema(description = "Kiểu token", example = "Bearer")
    private String type = "Bearer";

    @Schema(description = "Tên người dùng", example = "john_doe")
    private String username;

    @Schema(description = "Email của người dùng", example = "john_doe@example.com")
    private String email;

    @Schema(description = "Họ và tên đầy đủ", example = "John Doe")
    private String fullName;

    @Schema(description = "Vai trò của người dùng", example = "USER")
    private String role;

    @Schema(description = "Tổng điểm (chỉ cho USER)", example = "100")
    private Integer totalPoints;

    @Schema(description = "Số ngày học liên tiếp (chỉ cho USER)", example = "5")
    private Integer streakDays;

    // Constructor đầy đủ
    public AuthResponse(Long userId, String token, String username, String email,
            String fullName, String role, Integer totalPoints, Integer streakDays) {
        this.userId = userId;
        this.token = token;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.totalPoints = totalPoints;
        this.streakDays = streakDays;
    }

    // Constructor cho admin (không có points/streak)
    public AuthResponse(Long userId, String token, String username, String email,
            String fullName, String role) {
        this.userId = userId;
        this.token = token;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.totalPoints = null;
        this.streakDays = null;
    }
}