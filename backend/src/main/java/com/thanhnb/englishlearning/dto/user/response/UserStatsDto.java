package com.thanhnb.englishlearning.dto.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.thanhnb.englishlearning.entity.user.UserStats;

/**
 * DTO for UserStats entity
 * Used in API responses to return user statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thống kê người dùng")
public class UserStatsDto {

    @Schema(description = "ID người dùng", example = "1")
    private Long userId;

    // ==================== POINTS ====================

    @Schema(description = "Tổng điểm", example = "1500")
    private Integer totalPoints;

    @Schema(description = "Xếp hạng theo điểm", example = "15")
    private Long pointsRank;

    @Schema(description = "Phần trăm xếp hạng điểm", example = "85.5")
    private Double pointsPercentile;

    // ==================== STREAK ====================

    @Schema(description = "Số ngày liên tiếp hiện tại", example = "7")
    private Integer currentStreak;

    @Schema(description = "Số ngày liên tiếp cao nhất", example = "30")
    private Integer longestStreak;

    @Schema(description = "Ngày streak cuối cùng")
    private LocalDate lastStreakDate;

    @Schema(description = "Đã học hôm nay chưa", example = "true")
    private Boolean hasStreakToday;

    @Schema(description = "Xếp hạng theo streak", example = "8")
    private Long streakRank;

    // ==================== COMPLETION ====================

    @Schema(description = "Tổng bài học hoàn thành", example = "45")
    private Integer totalLessonsCompleted;

    @Schema(description = "Bài ngữ pháp hoàn thành", example = "15")
    private Integer grammarCompleted;

    @Schema(description = "Bài đọc hiểu hoàn thành", example = "20")
    private Integer readingCompleted;

    @Schema(description = "Bài nghe hoàn thành", example = "10")
    private Integer listeningCompleted;

    // ==================== STUDY TIME ====================

    @Schema(description = "Tổng thời gian học (phút)", example = "450")
    private Integer totalStudyTimeMinutes;

    @Schema(description = "Thời gian học trung bình mỗi bài (phút)", example = "10")
    private Integer averageSessionMinutes;

    // ==================== TIMESTAMPS ====================

    @Schema(description = "Thời điểm cập nhật cuối")
    private LocalDateTime lastUpdated;

    @Schema(description = "Thời điểm tạo")
    private LocalDateTime createdAt;

    // ==================== HELPER METHODS ====================

    /**
     * Get formatted study time (hours:minutes)
     */
    public String getFormattedStudyTime() {
        if (totalStudyTimeMinutes == null || totalStudyTimeMinutes == 0) {
            return "0h 0m";
        }
        int hours = totalStudyTimeMinutes / 60;
        int minutes = totalStudyTimeMinutes % 60;
        return String.format("%dh %dm", hours, minutes);
    }

    /**
     * Get completion percentage for specific module
     * (Requires total lessons available - would need to be passed in)
     */
    public Double getCompletionPercentage(String moduleType, Integer totalAvailable) {
        if (totalAvailable == null || totalAvailable == 0) {
            return 0.0;
        }

        Integer completed = switch (moduleType.toUpperCase()) {
            case "GRAMMAR" -> grammarCompleted;
            case "READING" -> readingCompleted;
            case "LISTENING" -> listeningCompleted;
            default -> 0;
        };

        return (completed * 100.0) / totalAvailable;
    }

    public static UserStatsDto from(UserStats stats) {
        if (stats == null) {
            return null;
        }

        return UserStatsDto.builder()
                .userId(stats.getUserId())
                .totalPoints(stats.getTotalPoints())
                .currentStreak(stats.getCurrentStreak())
                .longestStreak(stats.getLongestStreak())
                .lastStreakDate(stats.getLastStreakDate())
                .hasStreakToday(stats.hasStreakToday())
                .totalLessonsCompleted(stats.getTotalLessonsCompleted())
                .grammarCompleted(stats.getGrammarCompleted())
                .readingCompleted(stats.getReadingCompleted())
                .listeningCompleted(stats.getListeningCompleted())
                .totalStudyTimeMinutes(stats.getTotalStudyTimeMinutes())
                .averageSessionMinutes(stats.getAverageSessionMinutes())
                .lastUpdated(stats.getLastUpdated())
                .createdAt(stats.getCreatedAt())
                .build();
    }
}