package com.thanhnb.englishlearning.dto.user.response;

import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Complete user information including stats and activity
 * Used for profile page and detailed user views
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết người dùng (bao gồm stats và activity)")
public class UserDetailDto {

    // ==================== BASIC USER INFO ====================

    @Schema(description = "ID người dùng", example = "1")
    private Long id;

    @Schema(description = "Tên đăng nhập", example = "john_doe")
    private String username;

    @Schema(description = "Email", example = "john@example.com")
    private String email;

    @Schema(description = "Họ và tên", example = "John Doe")
    private String fullName;

    @Schema(description = "Vai trò", example = "USER")
    private UserRole role;

    @Schema(description = "Trình độ tiếng Anh", example = "INTERMEDIATE")
    private EnglishLevel englishLevel;

    @Schema(description = "Tài khoản đang hoạt động", example = "true")
    private Boolean isActive;

    @Schema(description = "Email đã xác thực", example = "true")
    private Boolean isVerified;

    @Schema(description = "Thời điểm tạo tài khoản")
    private LocalDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật")
    private LocalDateTime updatedAt;

    // ==================== STATS (Embedded) ====================

    @Schema(description = "Thống kê người dùng")
    private UserStatsDto stats;

    // ==================== ACTIVITY (Embedded) ====================

    @Schema(description = "Hoạt động người dùng")
    private UserActivityDto activity;

    // ==================== CONVENIENCE GETTERS ====================
    // For backward compatibility with old frontend code

    @Schema(description = "Tổng điểm (shortcut)", example = "1500")
    public Integer getTotalPoints() {
        return stats != null ? stats.getTotalPoints() : 0;
    }

    @Schema(description = "Streak hiện tại (shortcut)", example = "7")
    public Integer getStreakDays() {
        return stats != null ? stats.getCurrentStreak() : 0;
    }

    @Schema(description = "Ngày đăng nhập cuối (shortcut)")
    public LocalDateTime getLastLoginDate() {
        return activity != null ? activity.getLastLoginDate() : null;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Check if user is teacher
     */
    public boolean isTeacher() {
        return role == UserRole.TEACHER;
    }

    /**
     * Check if user is student
     */
    public boolean isStudent() {
        return role == UserRole.USER;
    }

    /**
     * Get display name (fullName or username)
     */
    public String getDisplayName() {
        return (fullName != null && !fullName.isEmpty()) ? fullName : username;
    }
}