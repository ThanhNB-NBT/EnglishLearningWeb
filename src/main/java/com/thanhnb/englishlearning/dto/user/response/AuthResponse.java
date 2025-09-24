package com.thanhnb.englishlearning.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    @Schema(description = "JWT token for authentication")
    private String token;

    @Schema(description = "Kiểu token", example = "Bearer")
    private String type = "Bearer";

    @Schema(description = "Tên người dùng", example = "john_doe")
    private String username;

    @Schema(description = "Email của người dùng", example = "john_doe@example.com")
    private String email;

    @Schema(description = "Họ và tên của người dùng", example = "John Doe")
    private String fullName;

    @Schema(description = "Vai trò của người dùng", example = "USER")
    private String role;

    public AuthResponse(String token, String username, String email, String fullName, String role) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
}
