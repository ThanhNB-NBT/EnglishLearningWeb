package com.thanhnb.englishlearning.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ✅ SIMPLIFIED Dashboard DTO - Only Real Data
 * 
 * Contains:
 * - User info (from User entity)
 * - Stats (from UserStats)
 * - Activity (from UserActivity)
 * - AI Recommendations (from AIRecommendation)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardDto {

    // ==================== CORE USER DATA ====================

    /**
     * User information with embedded stats and activity
     */
    private UserDetailDto user;

    // ==================== QUICK STATS (From UserStats) ====================

    /**
     * Quick statistics overview
     */
    private QuickStatsDto quickStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickStatsDto {
        private Integer currentStreak; // From UserStats
        private Integer totalPoints; // From UserStats
        private Integer totalLessonsCompleted; // From UserStats
        private Integer studyTimeToday; // Calculated from UserStats
        private Double weeklyGoalProgress; // Based on streak vs 7-day goal
    }

    // ==================== SKILL PROGRESS (From UserStats) ====================

    /**
     * Progress for each skill module
     * Map structure: { "grammar": {...}, "reading": {...}, "listening": {...} }
     */
    private java.util.Map<String, SkillProgressDto> skillProgress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillProgressDto {
        private Integer completed; // Số bài đã hoàn thành
        private Integer total; // Tổng số bài
        private String level; // Level của user trong skill này
        private Double accuracy; // Độ chính xác trung bình trong skill này
    }
    // ==================== STREAK INFO (From UserStats) ====================

    /**
     * Streak information
     */
    private StreakDto streak;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakDto {
        private Integer currentStreak;
        private Integer longestStreak;
        private String lastStreakDate;
        private Boolean hasStreakToday;
        private Integer streakGoal; // Default: 7 days
        private Double streakGoalProgress; // Progress toward goal (0-100)
    }
}