package com.thanhnb.englishlearning.dto.grammar;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupedQuestionsDTO;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import com.thanhnb.englishlearning.enums.LessonType;
import io.swagger.v3.oas.annotations.media.Schema;
import com.thanhnb.englishlearning.config.Views;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ UNIFIED DTO for ALL Grammar operations
 * - Create/Update (Admin/Teacher)
 * - List view (User)
 * - Detail view (User)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Grammar Lesson DTO - Bài học ngữ pháp")
public class GrammarLessonDTO {

    // ═══════════════════════════════════════════════════════════
    // CORE FIELDS (Required for create/update)
    // ═══════════════════════════════════════════════════════════

    @Schema(description = "ID bài học (null khi tạo mới)", example = "1")
    @JsonView(Views.Public.class)
    private Long id;

    @NotNull(message = "Topic ID không được để trống")
    @Schema(description = "ID chủ đề", example = "1")
    @JsonView(Views.Public.class)
    private Long topicId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không quá 200 ký tự")
    @Schema(description = "Tiêu đề bài học", example = "Present Simple Tense")
    @JsonView(Views.Public.class)
    private String title;

    @NotNull(message = "Loại bài học không được để trống")
    @Schema(description = "Loại bài học", example = "THEORY", allowableValues = { "THEORY", "PRACTICE" })
    @JsonView(Views.Public.class)
    private LessonType lessonType;

    @NotNull(message = "Thứ tự không được để trống")
    @Min(value = 1, message = "Thứ tự phải >= 1")
    @Schema(description = "Thứ tự bài học", example = "1")
    @JsonView(Views.Public.class)
    private Integer orderIndex;

    // ═══════════════════════════════════════════════════════════
    // OPTIONAL FIELDS
    // ═══════════════════════════════════════════════════════════

    @Schema(description = "Nội dung bài học (HTML/Markdown)", example = "<h1>Present Simple</h1><p>...</p>")
    @JsonView(Views.Public.class)
    private String content;

    @Min(value = 0, message = "Thời gian phải >= 0")
    @Schema(description = "Giới hạn thời gian (giây, 0 = không giới hạn)", example = "1800")
    @JsonView(Views.Public.class)
    private Integer timeLimitSeconds;

    @Min(value = 1, message = "Điểm thưởng phải >= 1")
    @Schema(description = "Điểm thưởng khi hoàn thành", example = "50")
    @JsonView(Views.Public.class)
    private Integer pointsReward;

    @Schema(description = "Trạng thái kích hoạt", example = "true")
    @JsonView(Views.Public.class)
    private Boolean isActive;

    // ═══════════════════════════════════════════════════════════
    // READ-ONLY FIELDS (Populated by system)
    // ═══════════════════════════════════════════════════════════

    @Schema(description = "Thời gian tạo", example = "2024-01-15T10:30:00")
    @JsonView(Views.Public.class)
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật", example = "2024-01-20T14:45:00")
    @JsonView(Views.Public.class)
    private LocalDateTime modifiedAt;

    @Schema(description = "ID người cập nhật cuối", example = "5")
    @JsonView(Views.Public.class)
    private Long modifiedBy;

    @Schema(description = "Tên chủ đề", example = "Basic Tenses")
    @JsonView(Views.Public.class)
    private String topicName;

    @Schema(description = "Số lượng câu hỏi", example = "10")
    @JsonView(Views.Public.class)
    private Integer questionCount;

    @Schema(description = "Trình độ yêu cầu", example = "A1")
    @JsonView(Views.Public.class)
    private EnglishLevel requiredLevel;

    // ═══════════════════════════════════════════════════════════
    // USER PROGRESS (For user-facing endpoints)
    // ═══════════════════════════════════════════════════════════

    @Schema(description = "Đã hoàn thành chưa", example = "true")
    @JsonView(Views.Public.class)
    private Boolean isCompleted;

    @Schema(description = "Đã mở khóa chưa", example = "true")
    @JsonView(Views.Public.class)
    private Boolean isUnlocked;

    @Schema(description = "Có thể truy cập không", example = "true")
    @JsonView(Views.Public.class)
    private Boolean isAccessible;

    @Schema(description = "Điểm của user", example = "85")
    @JsonView(Views.Public.class)
    private Integer userScore;

    @Schema(description = "Số lần thử", example = "3")
    @JsonView(Views.Public.class)
    private Integer userAttempts;

    @Schema(description = "Thời gian hoàn thành", example = "2024-01-18T16:20:00")
    @JsonView(Views.Public.class)
    private LocalDateTime completedAt;

    // ═══════════════════════════════════════════════════════════
    // QUESTIONS (For detail view)
    // ═══════════════════════════════════════════════════════════

    @Deprecated
    @Schema(description = "⚠️ DEPRECATED: Dùng groupedQuestions thay thế")
    @JsonView(Views.Public.class)
    private List<QuestionResponseDTO> questions;

    @Schema(description = "✅ Câu hỏi theo cấu trúc Task (flat hoặc grouped)")
    @JsonView(Views.Public.class)
    private TaskGroupedQuestionsDTO groupedQuestions;

    // ═════════════════════════════════════════════════════════════════
    // AI IMPORT FIELDS (Temporary - only used during import flow)
    // ═════════════════════════════════════════════════════════════════

    @Schema(description = "Task groups with questions (AI import only)")
    @JsonView(Views.Admin.class) // Chỉ admin thấy
    private List<TaskGroupImportData> taskGroups;

    @Schema(description = "Standalone questions (AI import only)")
    @JsonView(Views.Admin.class)
    private List<CreateQuestionDTO> standaloneQuestions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskGroupImportData {
        private String taskName;
        private String instruction;
        private Integer orderIndex;
        private List<CreateQuestionDTO> questions;
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Add questions and auto-calculate count
     */
    @Deprecated
    public GrammarLessonDTO withQuestions(List<QuestionResponseDTO> questions) {
        this.questions = questions;
        this.questionCount = questions != null ? questions.size() : 0;
        return this;
    }

    public GrammarLessonDTO withGroupedQuestions(TaskGroupedQuestionsDTO groupedQuestions) {
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
    public GrammarLessonDTO withProgress(
            Boolean isCompleted, Integer userScore,
            Integer userAttempts, LocalDateTime completedAt) {
        this.isCompleted = isCompleted;
        this.userScore = userScore;
        this.userAttempts = userAttempts;
        this.completedAt = completedAt;
        return this;
    }

    /**
     * Add unlock status
     */
    public GrammarLessonDTO withUnlockStatus(Boolean isUnlocked, Boolean isAccessible) {
        this.isUnlocked = isUnlocked;
        this.isAccessible = isAccessible;
        return this;
    }
}