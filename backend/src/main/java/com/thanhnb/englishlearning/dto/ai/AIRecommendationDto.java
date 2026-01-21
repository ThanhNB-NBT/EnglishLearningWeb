package com.thanhnb.englishlearning.dto.ai;

import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.ModuleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ✅ ENHANCED AI Recommendation Response DTO
 * 
 * Includes:
 * - Basic recommendation info
 * - Target lesson/topic details
 * - Progress & completion info
 * - Smart badges & tags
 * - Estimated time & difficulty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI Recommendation with full details")
public class AIRecommendationDto {

    // ==================== BASIC INFO ====================

    @Schema(description = "Recommendation ID", example = "123")
    private Long id;

    @Schema(description = "Recommendation type", example = "PRACTICE_WEAK_SKILL")
    private String type;

    @Schema(description = "Title (Vietnamese)", example = "💪 Cải thiện Grammar")
    private String title;

    @Schema(description = "Description (Vietnamese)")
    private String description;

    @Schema(description = "AI reasoning (English)")
    private String reasoning;

    @Schema(description = "Priority level (1-5)", example = "5")
    private Integer priority;

    @Schema(description = "Target skill", example = "GRAMMAR")
    private ModuleType targetSkill;

    // ==================== TARGET DETAILS ====================

    @Schema(description = "Target lesson ID", example = "45")
    private Long targetLessonId;

    @Schema(description = "Target lesson title", example = "Present Perfect Tense")
    private String targetLessonTitle;

    @Schema(description = "Target topic ID", example = "12")
    private Long targetTopicId;

    @Schema(description = "Target topic name", example = "Verb Tenses")
    private String targetTopicName;

    @Schema(description = "Lesson difficulty", example = "INTERMEDIATE")
    private EnglishLevel lessonDifficulty;

    @Schema(description = "Estimated completion time (minutes)", example = "15")
    private Integer estimatedMinutes;

    @Schema(description = "Total questions in lesson", example = "10")
    private Integer totalQuestions;

    // ==================== PROGRESS INFO ====================

    @Schema(description = "Topic completion percentage", example = "45.5")
    private Double topicCompletionPercentage;

    @Schema(description = "Number of attempts on this lesson", example = "2")
    private Integer attemptsCount;

    @Schema(description = "Best score on this lesson (%)", example = "75.0")
    private Double bestScore;

    @Schema(description = "Average accuracy on this topic (%)", example = "68.5")
    private Double topicAverageAccuracy;

    // ==================== STATUS FLAGS ====================

    @Schema(description = "Has been shown to user", example = "true")
    private Boolean isShown;

    @Schema(description = "User accepted/clicked", example = "false")
    private Boolean isAccepted;

    @Schema(description = "User completed", example = "false")
    private Boolean isCompleted;

    @Schema(description = "Already attempted before", example = "true")
    private Boolean isPreviouslyAttempted;

    @Schema(description = "Part of current learning path", example = "true")
    private Boolean isInLearningPath;

    // ==================== SMART BADGES ====================

    @Schema(description = "Smart badges", example = "[\"🔥 HOT_STREAK\", \"⚡ QUICK_WIN\"]")
    private java.util.List<String> badges;

    @Schema(description = "Time-based tag", example = "☀️ MORNING_OPTIMAL")
    private String timeTag;

    @Schema(description = "Motivation message", example = "You're on fire! 🔥")
    private String motivationMessage;

    // ==================== METADATA ====================

    @Schema(description = "Points awarded on completion", example = "50")
    private Integer pointsReward;

    @Schema(description = "Expiration date")
    private LocalDateTime expiresAt;

    @Schema(description = "Created date")
    private LocalDateTime createdAt;

    @Schema(description = "Shown at")
    private LocalDateTime shownAt;

    // ==================== SOCIAL PROOF ====================

    @Schema(description = "Success rate of this lesson (%)", example = "85.5")
    private Double lessonSuccessRate;

    @Schema(description = "Average completion time (minutes)", example = "12")
    private Integer avgCompletionTime;

    @Schema(description = "Number of users completed", example = "1250")
    private Integer usersCompleted;

    @Schema(description = "User rating (1-5)", example = "4.5")
    private Double userRating;

    // ==================== HELPER METHODS ====================

    /**
     * Get priority label
     */
    public String getPriorityLabel() {
        if (priority == null)
            return "NORMAL";
        return switch (priority) {
            case 5 -> "URGENT";
            case 4 -> "HIGH";
            case 3 -> "NORMAL";
            case 2 -> "LOW";
            default -> "OPTIONAL";
        };
    }

    /**
     * Get difficulty label
     */
    public String getDifficultyLabel() {
        if (lessonDifficulty == null)
            return "Unknown";
        return switch (lessonDifficulty) {
            case A1 -> "Beginner";
            case A2 -> "Elementary";
            case B1 -> "Intermediate";
            case B2 -> "Upper-Intermediate";
            case C1 -> "Advanced";
            case C2 -> "Proficient";
        };
    }

    /**
     * Check if this is a quick win (< 10 minutes)
     */
    public Boolean isQuickWin() {
        return estimatedMinutes != null && estimatedMinutes <= 10;
    }

    /**
     * Check if highly rated
     */
    public Boolean isHighlyRated() {
        return userRating != null && userRating >= 4.5;
    }

    /**
     * Get completion status message
     */
    public String getCompletionStatusMessage() {
        if (isCompleted != null && isCompleted) {
            return "✅ Completed";
        }
        if (isPreviouslyAttempted != null && isPreviouslyAttempted) {
            return "🔄 In Progress";
        }
        return "🆕 New";
    }
}