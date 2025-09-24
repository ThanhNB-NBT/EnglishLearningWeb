package com.thanhnb.englishlearning.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class VerifyOtpRequest {
    
    @NotBlank(message = "Email không được để trống")
    @Schema(description = "Email của người dùng", example = "john_doe@example.com")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "OTP không được để trống")
    @Pattern(regexp = "\\d{6}", message = "OTP phải là 6 chữ số")
    @Schema(description = "OTP của người dùng", example = "123456")
    private String otp;
}
