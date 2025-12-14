package com.thanhnb.englishlearning.dto. listening;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thanhnb.englishlearning. dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto. question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.entity.listening.ListeningLesson. Difficulty;
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
@Schema(description = "Listening lesson information")
public class ListeningLessonDTO {

    // BASIC INFO (for both Summary and Full)
    @Schema(description = "Lesson ID", example = "1")
    private Long id;

    @NotBlank(message = "Title cannot be empty")
    @Size(max = 200, message = "Title max 200 characters")
    @Schema(description = "Lesson title", example = "Daily Conversation at a Cafe")
    private String title;

    @Schema(description = "Audio file URL", example = "/media/listening/lesson_1/audio.mp3")
    private String audioUrl;

    @NotNull(message = "Difficulty cannot be null")
    @Schema(description = "Difficulty level", example = "INTERMEDIATE")
    private Difficulty difficulty;

    @NotNull(message = "Order index cannot be null")
    @Min(value = 1, message = "Order index must be >= 1")
    @Schema(description = "Display order", example = "1")
    private Integer orderIndex;

    @Min(value = 60, message = "Time limit must be >= 60 seconds")
    @Max(value = 3600, message = "Time limit must not exceed 3600 seconds")
    @Schema(description = "Time limit in seconds", example = "600")
    private Integer timeLimitSeconds = 600;

    @Min(value = 1, message = "Points reward must be >= 1")
    @Max(value = 100, message = "Points reward must not exceed 100")
    @Schema(description = "Points when completed", example = "25")
    private Integer pointsReward = 25;

    @Schema(description = "Allow unlimited replay", example = "true")
    private Boolean allowUnlimitedReplay = true;

    @Min(value = 1, message = "Max replay count must be >= 1")
    @Max(value = 10, message = "Max replay count must not exceed 10")
    @Schema(description = "Max replay count", example = "3")
    private Integer maxReplayCount = 3;

    @Schema(description = "Active status", example = "true")
    private Boolean isActive = true;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Number of questions", example = "10")
    private Integer questionCount;

    // FULL DETAILS (only when loading full lesson)
    @Schema(description = "English transcript")
    private String transcript;

    @Schema(description = "Vietnamese translation")
    private String transcriptTranslation;

    @Schema(description = "Transcript unlocked status", example = "false")
    private Boolean transcriptUnlocked;

    // Questions
    @Schema(description = "List of questions (response)")
    private List<QuestionResponseDTO> questions;

    @Schema(description = "List of questions (create)")
    private List<CreateQuestionDTO> createQuestions;

    // USER PROGRESS (only when user logged in)
    @Schema(description = "Is unlocked", example = "false")
    private Boolean isUnlocked;

    @Schema(description = "Is accessible", example = "false")
    private Boolean isAccessible;

    @Schema(description = "Is completed", example = "false")
    private Boolean isCompleted;

    @Schema(description = "User score percentage", example = "85. 5")
    private Double scorePercentage;

    @Schema(description = "Number of attempts", example = "2")
    private Integer attempts;

    @Schema(description = "Play count", example = "5")
    private Integer playCount;

    @Schema(description = "Remaining replays")
    private Integer remainingReplays;

    @Schema(description = "Has viewed transcript", example = "false")
    private Boolean hasViewedTranscript;

    @Schema(description = "Completion timestamp")
    private LocalDateTime completedAt;

    // STATIC FACTORY METHODS

    /**
     * Create Summary DTO (for list view)
     */
    public static ListeningLessonDTO summary(Long id, String title, Difficulty difficulty,
            Integer orderIndex, Integer pointsReward, Boolean isActive, Integer questionCount) {
        ListeningLessonDTO dto = new ListeningLessonDTO();
        dto.setId(id);
        dto.setTitle(title);
        dto.setDifficulty(difficulty);
        dto.setOrderIndex(orderIndex);
        dto.setPointsReward(pointsReward);
        dto.setIsActive(isActive);
        dto.setQuestionCount(questionCount);
        return dto;
    }

    /**
     * Create Full DTO (for detail view)
     */
    public static ListeningLessonDTO full(Long id, String title, String audioUrl,
            String transcript, String transcriptTranslation, Difficulty difficulty,
            Integer timeLimitSeconds, Integer orderIndex, Integer pointsReward, 
            Boolean allowUnlimitedReplay, Integer maxReplayCount,
            Boolean isActive, LocalDateTime createdAt) {
        ListeningLessonDTO dto = new ListeningLessonDTO();
        dto.setId(id);
        dto.setTitle(title);
        dto.setAudioUrl(audioUrl);
        dto.setTranscript(transcript);
        dto.setTranscriptTranslation(transcriptTranslation);
        dto.setDifficulty(difficulty);
        dto.setTimeLimitSeconds(timeLimitSeconds);
        dto.setOrderIndex(orderIndex);
        dto.setPointsReward(pointsReward);
        dto.setAllowUnlimitedReplay(allowUnlimitedReplay);
        dto.setMaxReplayCount(maxReplayCount);
        dto.setIsActive(isActive);
        dto.setCreatedAt(createdAt);
        return dto;
    }

    /**
     * Add user progress to DTO
     */
    public ListeningLessonDTO withProgress(Boolean isCompleted, Double scorePercentage,
            Integer attempts, Integer playCount, Boolean hasViewedTranscript, 
            LocalDateTime completedAt) {
        this.isCompleted = isCompleted;
        this.scorePercentage = scorePercentage;
        this.attempts = attempts;
        this.playCount = playCount;
        this.hasViewedTranscript = hasViewedTranscript;
        this.completedAt = completedAt;
        return this;
    }

    /**
     * Set questions list and update question count
     */
    public ListeningLessonDTO withQuestions(List<QuestionResponseDTO> questions) {
        this.questions = questions;
        this.questionCount = questions != null ? questions.size() : 0;
        return this;
    }

    /**
     * Set unlock status
     */
    public ListeningLessonDTO withUnlockStatus(boolean isUnlocked, boolean isAccessible) {
        this.isUnlocked = isUnlocked;
        this. isAccessible = isAccessible;
        return this;
    }

    /**
     * Set replay info
     */
    public ListeningLessonDTO withReplayInfo(Integer remainingReplays) {
        this.remainingReplays = remainingReplays;
        return this;
    }

    /**
     * Set transcript unlock status
     */
    public ListeningLessonDTO withTranscriptUnlocked(boolean transcriptUnlocked) {
        this.transcriptUnlocked = transcriptUnlocked;
        return this;
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get questions count
     */
    public int getQuestionCount() {
        if (questionCount != null) {
            return questionCount;
        }
        if (createQuestions != null) {
            return createQuestions.size();
        }
        if (questions != null) {
            return questions.size();
        }
        return 0;
    }

    /**
     * Check if lesson has questions
     */
    public boolean hasQuestions() {
        return getQuestionCount() > 0;
    }
}