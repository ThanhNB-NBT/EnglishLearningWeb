package com.thanhnb.englishlearning.dto.user.request;

import com.thanhnb.englishlearning.enums.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    @Schema(description = "Tên đăng nhập của người dùng", example = "john_doe")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Schema(description = "Email của người dùng", example = "john_doe@example.com")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, max = 100, message = "Password phải từ 6-100 ký tự")
    @Schema(description = "Mật khẩu của người dùng", example = "password123")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Schema(description = "Họ tên của người dùng", example = "John Doe")
    private String fullName;

    @Schema(description = "Vai trò của người dùng", example = "USER")
    private UserRole role;
}
