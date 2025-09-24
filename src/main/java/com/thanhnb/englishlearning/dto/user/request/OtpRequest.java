package com.thanhnb.englishlearning.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class OtpRequest {
    
    @NotBlank(message = "Email không được để trống")
    @Schema(description = "Email của người dùng", example = "john_doe@example.com")
    @Email(message = "Email không hợp lệ")
    private String email;

}
