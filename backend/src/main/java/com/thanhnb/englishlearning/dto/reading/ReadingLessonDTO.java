package com.thanhnb.englishlearning.dto.reading;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupedQuestionsDTO;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.config.Views;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ SINGLE DTO for ALL operations: Create, Update, Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadingLessonDTO {

    // ═════════════════════════════════════════════════════════════════
    // CORE FIELDS
    // ═════════════════════════════════════════════════════════════════

    @JsonView(Views.Public.class)
    private Long id;

    @NotNull(message = "Topic ID is required")
    @JsonView(Views.Public.class)
    private Long topicId;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    @JsonView(Views.Public.class)
    private String title;

    @NotBlank(message = "Content is required")
    @JsonView(Views.Public.class)
    private String content; // Main reading passage

    @NotNull(message = "Order index is required")
    @Min(1)
    @JsonView(Views.Public.class)
    private Integer orderIndex;

    // ═════════════════════════════════════════════════════════════════
    // OPTIONAL FIELDS
    // ═════════════════════════════════════════════════════════════════

    @JsonView(Views.Public.class)
    private String contentTranslation; // Vietnamese translation

    @Min(60)
    @JsonView(Views.Public.class)
    private Integer timeLimitSeconds;

    @Min(1)
    @JsonView(Views.Public.class)
    private Integer pointsReward;

    @JsonView(Views.Public.class)
    private Boolean isActive;

    // ═════════════════════════════════════════════════════════════════
    // READ-ONLY FIELDS
    // ═════════════════════════════════════════════════════════════════

    @JsonView(Views.Public.class)
    private LocalDateTime createdAt;
    
    @JsonView(Views.Public.class)
    private LocalDateTime modifiedAt;

    @JsonView(Views.Public.class)
    private Long modifiedBy;

    @JsonView(Views.Public.class)
    private String topicName;

    @JsonView(Views.Public.class)
    private Integer questionCount;

    @JsonView(Views.Public.class)
    private EnglishLevel requiredLevel;

    // ═════════════════════════════════════════════════════════════════
    // USER PROGRESS
    // ═════════════════════════════════════════════════════════════════

    @JsonView(Views.Public.class)
    private Boolean isCompleted;

    @JsonView(Views.Public.class)
    private Boolean isUnlocked;

    @JsonView(Views.Public.class)
    private Boolean isAccessible;

    @JsonView(Views.Public.class)
    private Double scorePercentage;

    @JsonView(Views.Public.class)
    private Integer attempts;

    @JsonView(Views.Public.class)
    private LocalDateTime completedAt;

    // ═════════════════════════════════════════════════════════════════
    // QUESTIONS (For detail view)
    // ═════════════════════════════════════════════════════════════════

    @Deprecated
    @JsonView(Views.Public.class)
    private List<QuestionResponseDTO> questions;

    @JsonView(Views.Public.class)
    private TaskGroupedQuestionsDTO groupedQuestions;

    // ═════════════════════════════════════════════════════════════════
    // STATIC FACTORY METHODS
    // ═════════════════════════════════════════════════════════════════

    /**
     * Create summary DTO for list view
     */
    public static ReadingLessonDTO summary(
            Long id, String title, Integer timeLimitSeconds,
            Integer orderIndex, Boolean isActive, Integer questionCount) {
        return ReadingLessonDTO.builder()
                .id(id)
                .title(title)
                .timeLimitSeconds(timeLimitSeconds)
                .orderIndex(orderIndex)
                .isActive(isActive)
                .questionCount(questionCount)
                .build();
    }

    /**
     * Create full DTO for detail view
     */
    public static ReadingLessonDTO full(
            Long id, String title, String content, String contentTranslation,
            Integer timeLimitSeconds, Integer orderIndex, Integer pointsReward,
            Boolean isActive, LocalDateTime createdAt) {
        return ReadingLessonDTO.builder()
                .id(id)
                .title(title)
                .content(content)
                .contentTranslation(contentTranslation)
                .timeLimitSeconds(timeLimitSeconds)
                .orderIndex(orderIndex)
                .pointsReward(pointsReward)
                .isActive(isActive)
                .createdAt(createdAt)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════
    // FLUENT BUILDER METHODS (For service layer)
    // ═════════════════════════════════════════════════════════════════

    /**
     * Add questions to DTO
     */
    @Deprecated
    public ReadingLessonDTO withQuestions(List<QuestionResponseDTO> questions) {
        this.questions = questions;
        this.questionCount = questions != null ? questions.size() : 0;
        return this;
    }

    /**
     * ✅ NEW: Add grouped questions structure
     */
    public ReadingLessonDTO withGroupedQuestions(TaskGroupedQuestionsDTO groupedQuestions) {
        this.groupedQuestions = groupedQuestions;

        // Calculate total question count
        if (groupedQuestions != null) {
            int count = 0;

            if (groupedQuestions.getStandaloneQuestions() != null) {
                count += groupedQuestions.getStandaloneQuestions().size();
            }

            if (groupedQuestions.getTasks() != null) {
                count += groupedQuestions.getTasks().stream()
                        .mapToInt(task -> task.getQuestions() != null ? task.getQuestions().size() : 0)
                        .sum();
            }

            this.questionCount = count;
        }

        return this;
    }

    /**
     * Add progress information
     */
    public ReadingLessonDTO withProgress(
            Boolean isCompleted, Double scorePercentage,
            Integer attempts, LocalDateTime completedAt) {
        this.isCompleted = isCompleted;
        this.scorePercentage = scorePercentage;
        this.attempts = attempts;
        this.completedAt = completedAt;
        return this;
    }

    /**
     * Add unlock status
     */
    public ReadingLessonDTO withUnlockStatus(Boolean isUnlocked, Boolean isAccessible) {
        this.isUnlocked = isUnlocked;
        this.isAccessible = isAccessible;
        return this;
    }
}