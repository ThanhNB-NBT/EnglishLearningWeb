package com.thanhnb.englishlearning.dto.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response khi đăng nhập thành công")
public class AuthResponse {

    @Schema(description = "ID người dùng", example = "1")
    private Long userId;

    @Schema(description = "JWT token")
    private String token;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email", example = "john@example.com")
    private String email;

    @Schema(description = "Họ tên", example = "John Doe")
    private String fullName;

    @Schema(description = "Vai trò", example = "USER")
    private String role;

    // ==================== USER-ONLY FIELDS ====================

    @Schema(description = "Thống kê người dùng (chỉ có khi role=USER)")
    private UserStatsDto stats;

    @Deprecated
    @Schema(description = "Tổng điểm (deprecated - dùng stats.totalPoints)", example = "1500")
    private Integer totalPoints;

    @Deprecated
    @Schema(description = "Streak days (deprecated - dùng stats.currentStreak)", example = "7")
    private Integer streakDays;

    // ==================== CONSTRUCTORS ====================

    /**
     * Constructor for ADMIN login
     * Admin doesn't have stats
     */
    public AuthResponse(Long userId, String token, String username, String email,
            String fullName, String role) {
        this.userId = userId;
        this.token = token;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.stats = null;
        this.totalPoints = null;
        this.streakDays = null;
    }

    /**
     * Constructor for USER login
     * Includes stats
     */
    public AuthResponse(Long userId, String token, String username, String email,
            String fullName, String role, UserStatsDto stats) {
        this.userId = userId;
        this.token = token;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.stats = stats;

        // Set deprecated fields for backward compatibility
        if (stats != null) {
            this.totalPoints = stats.getTotalPoints();
            this.streakDays = stats.getCurrentStreak();
        }
    }

    /**
     * Old constructor for backward compatibility
     * 
     * @deprecated Use constructor with UserStatsDto instead
     */
    @Deprecated
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

        // Build basic stats object
        if (totalPoints != null || streakDays != null) {
            this.stats = UserStatsDto.builder()
                    .userId(userId)
                    .totalPoints(totalPoints != null ? totalPoints : 0)
                    .currentStreak(streakDays != null ? streakDays : 0)
                    .build();
        }
    }

    // ==================== HELPER METHODS ====================

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isTeacher() {
        return "TEACHER".equals(role);
    }

    public boolean isUser() {
        return "USER".equals(role);
    }
}