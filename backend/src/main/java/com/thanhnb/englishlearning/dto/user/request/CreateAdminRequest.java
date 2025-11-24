package com.thanhnb.englishlearning.dto.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAdminRequest {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank(message = "Full name không được để trống")
    private String fullName;
}
