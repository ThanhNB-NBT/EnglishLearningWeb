package com.thanhnb.englishlearning.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class LoginRequest {
    @NotBlank(message = "Username hoặc email không được để trống")
    @Schema(description = "Tên đăng nhập hoặc email của người dùng", example = "john_doe")
    private String usernameOrEmail;

    @NotBlank(message = "Password không được để trống")
    @Schema(description = "Mật khẩu của người dùng", example = "password123")
    private String password;
}
