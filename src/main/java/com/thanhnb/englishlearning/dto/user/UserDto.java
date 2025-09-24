package com.thanhnb.englishlearning.dto.user;

import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin người dùng")
public class UserDto {

    @Schema(description = "ID của người dùng")
    private Long id;

    @NotBlank(message = "Tên tài khoản không được để trống")
    @Size(min = 3, max = 50, message = "Tên tài khoản phải từ 3 đến 50 ký tự")
    @Schema(description = "Tên tài khoản của người dùng", example = "john_doe")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Schema(description = "Email của người dùng", example = "john_doe@example.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @Schema(description = "Mật khẩu của người dùng", example = "password123")
    private String password;

    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    @Schema(description = "Họ và tên của người dùng", example = "John Doe")
    private String fullName;

    @Schema(description = "Vai trò của người dùng", example = "USER")
    private UserRole role;
    
    @Schema(description = "Cấp độ tiếng Anh của người dùng", example = "BEGINNER")
    private EnglishLevel englishLevel = EnglishLevel.BEGINNER;
    
    @Schema(description = "Tổng điểm của người dùng", example = "100")
    private Integer totalPoints = 0;

    @Schema(description = "Số ngày liên tiếp đăng nhập", example = "5")
    private Integer streakDays = 0;
    
    @Schema(description = "Thời điểm đăng nhập lần cuối")
    private LocalDateTime lastLoginDate;

    @Schema(description = "Thời điểm tạo người dùng")
    private LocalDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật người dùng")
    private LocalDateTime updatedAt;

    @Schema(description = "Trạng thái hoạt động của người dùng", example = "true")
    private Boolean isActive = true;

}
