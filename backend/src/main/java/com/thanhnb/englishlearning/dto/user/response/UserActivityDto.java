package com.thanhnb.englishlearning.dto.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for UserActivity entity
 * Used in API responses to return user activity info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hoạt động người dùng")
public class UserActivityDto {

    @Schema(description = "ID người dùng", example = "1")
    private Long userId;

    // ==================== LOGIN TRACKING ====================

    @Schema(description = "Thời điểm đăng nhập cuối")
    private LocalDateTime lastLoginDate;

    @Schema(description = "Thời điểm hoạt động cuối")
    private LocalDateTime lastActivityDate;

    @Schema(description = "Số lần đăng nhập", example = "50")
    private Integer loginCount;

    // ==================== SESSION INFO ====================

    @Schema(description = "IP đăng nhập cuối", example = "192.168.1.1")
    private String lastLoginIp;

    @Schema(description = "User Agent cuối")
    private String lastUserAgent;

    // ==================== DERIVED FIELDS ====================

    @Schema(description = "Số giây từ lần hoạt động cuối", example = "300")
    private Long secondsSinceLastActivity;

    @Schema(description = "Số giây từ lần đăng nhập cuối", example = "3600")
    private Long secondsSinceLastLogin;

    @Schema(description = "Đang hoạt động (trong 5 phút)", example = "true")
    private Boolean isCurrentlyActive;

    // ==================== TIMESTAMPS ====================

    @Schema(description = "Thời điểm cập nhật")
    private LocalDateTime updatedAt;

    @Schema(description = "Thời điểm tạo")
    private LocalDateTime createdAt;

    // ==================== HELPER METHODS ====================

    /**
     * Get formatted time since last activity
     */
    public String getFormattedTimeSinceActivity() {
        if (secondsSinceLastActivity == null) {
            return "N/A";
        }
        
        if (secondsSinceLastActivity < 60) {
            return secondsSinceLastActivity + " giây trước";
        } else if (secondsSinceLastActivity < 3600) {
            return (secondsSinceLastActivity / 60) + " phút trước";
        } else if (secondsSinceLastActivity < 86400) {
            return (secondsSinceLastActivity / 3600) + " giờ trước";
        } else {
            return (secondsSinceLastActivity / 86400) + " ngày trước";
        }
    }

    /**
     * Get browser name from User Agent (simple version)
     */
    public String getBrowserName() {
        if (lastUserAgent == null || lastUserAgent.isEmpty()) {
            return "Unknown";
        }
        
        String ua = lastUserAgent.toLowerCase();
        if (ua.contains("chrome")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari")) return "Safari";
        if (ua.contains("edge")) return "Edge";
        if (ua.contains("opera")) return "Opera";
        return "Other";
    }
}