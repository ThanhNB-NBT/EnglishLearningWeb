package com.thanhnb.englishlearning.dto.reading;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thanhnb.englishlearning.dto.question.QuestionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Reading lesson information")
public class ReadingLessonDTO {

    // BASIC INFO (for both Summary and Full)
    @Schema(description = "Lesson ID", example = "1")
    private Long id;

    @NotBlank(message = "Title cannot be empty")
    @Size(max = 200, message = "Title max 200 characters")
    @Schema(description = "Lesson title", example = "The Digital Paradox")
    private String title;

    @NotNull(message = "Order index cannot be null")
    @Min(value = 1, message = "Order index must be >= 1")
    @Schema(description = "Display order", example = "1")
    private Integer orderIndex;

    @Min(value = 1, message = "Points reward must be >= 1")
    @Schema(description = "Points when completed", example = "25")
    private Integer pointsReward = 25;

    @Schema(description = "Active status", example = "true")
    private Boolean isActive = true;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Number of questions", example = "10")
    private Integer questionCount;

    // FULL DETAILS (only when loading full lesson)
    @Schema(description = "English content (plain text)")
    private String content;

    @Schema(description = "Vietnamese translation (plain text)")
    private String contentTranslation;

    @Schema(description = "List of questions")
    private List<QuestionDTO> questions;

    // USER PROGRESS (only when user logged in)
    @Schema(description = "Is unlocked", example = "false")
    private Boolean isUnlocked;

    @Schema(description = "Is accessible", example = "false")
    private Boolean isAccessible;

    @Schema(description = "Is completed", example = "false")
    private Boolean isCompleted;

    @Schema(description = "User score percentage", example = "85.5")
    private Double scorePercentage;

    @Schema(description = "Number of attempts", example = "2")
    private Integer attempts;

    @Schema(description = "Completion timestamp")
    private LocalDateTime completedAt;

    // STATIC FACTORY METHODS

    /**
     * Create Summary DTO (for list view)
     */
    public static ReadingLessonDTO summary(Long id, String title, Integer orderIndex, 
            Boolean isActive, Integer questionCount) {
        ReadingLessonDTO dto = new ReadingLessonDTO();
        dto.setId(id);
        dto.setTitle(title);
        dto.setOrderIndex(orderIndex);
        dto.setIsActive(isActive);
        dto.setQuestionCount(questionCount);
        return dto;
    }

    /**
     * Create Full DTO (for detail view)
     */
    public static ReadingLessonDTO full(Long id, String title, String content, 
            String contentTranslation, Integer orderIndex, Integer pointsReward, 
            Boolean isActive, LocalDateTime createdAt) {
        ReadingLessonDTO dto = new ReadingLessonDTO();
        dto.setId(id);
        dto.setTitle(title);
        dto.setContent(content);
        dto.setContentTranslation(contentTranslation);
        dto.setOrderIndex(orderIndex);
        dto.setPointsReward(pointsReward);
        dto.setIsActive(isActive);
        dto.setCreatedAt(createdAt);
        return dto;
    }

    /**
     * Add user progress to DTO
     */
    public ReadingLessonDTO withProgress(Boolean isCompleted, Double scorePercentage, 
            Integer attempts, LocalDateTime completedAt) {
        this.isCompleted = isCompleted;
        this.scorePercentage = scorePercentage;
        this.attempts = attempts;
        this.completedAt = completedAt;
        return this;
    }

    /**
     * ✅ Set questions list và update question count
     */
    public ReadingLessonDTO withQuestions(List<QuestionDTO> questions) {
        this.questions = questions;
        this.questionCount = questions != null ? questions.size() : 0;
        return this;
    }

    /**
     * ✅ Set unlock status
     */
    public ReadingLessonDTO withUnlockStatus(boolean isUnlocked, boolean isAccessible) {
        this.isUnlocked = isUnlocked;
        this.isAccessible = isAccessible;
        return this;
    }
}