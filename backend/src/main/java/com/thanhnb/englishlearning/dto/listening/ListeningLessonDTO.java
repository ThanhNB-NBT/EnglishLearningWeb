package com.thanhnb.englishlearning.dto.listening;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.dto.question.response.TaskGroupedQuestionsDTO;
import com.thanhnb.englishlearning.enums.EnglishLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ UNIFIED DTO for ALL Listening operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Listening Lesson DTO - Bài học nghe")
public class ListeningLessonDTO {

    // ═══════════════════════════════════════════════════════════
    // CORE FIELDS
    // ═══════════════════════════════════════════════════════════
    
    @Schema(description = "ID bài học", example = "1")
    @JsonView(Views.Public.class)
    private Long id;
    
    @NotNull(message = "Topic ID không được để trống")
    @Schema(description = "ID chủ đề", example = "1")
    @JsonView(Views.Public.class)
    private Long topicId;
    
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề không quá 200 ký tự")
    @Schema(description = "Tiêu đề bài học", example = "Daily Conversation")
    @JsonView(Views.Public.class)
    private String title;
    
    @NotNull(message = "Thứ tự không được để trống")
    @Min(value = 1, message = "Thứ tự phải >= 1")
    @Schema(description = "Thứ tự bài học", example = "1")
    @JsonView(Views.Public.class)
    private Integer orderIndex;

    // ═══════════════════════════════════════════════════════════
    // AUDIO FIELDS
    // ═══════════════════════════════════════════════════════════
    
    @Schema(description = "URL file audio", example = "https://storage.example.com/audio/lesson1.mp3")
    @JsonView(Views.Public.class)
    private String audioUrl;
    
    @NotBlank(message = "Transcript không được để trống")
    @Schema(description = "Transcript (bản ghi âm)", example = "Hello, how are you today?")
    @JsonView(Views.Public.class)
    private String transcript;
    
    @Schema(description = "Bản dịch transcript", example = "Xin chào, hôm nay bạn thế nào?")
    @JsonView(Views.Public.class)
    private String transcriptTranslation;

    // ═══════════════════════════════════════════════════════════
    // OPTIONAL FIELDS
    // ═══════════════════════════════════════════════════════════
    
    @Min(value = 60, message = "Thời gian phải >= 60 giây")
    @Schema(description = "Giới hạn thời gian (giây)", example = "600")
    @JsonView(Views.Public.class)
    private Integer timeLimitSeconds;
    
    @Min(value = 1, message = "Điểm thưởng phải >= 1")
    @Schema(description = "Điểm thưởng khi hoàn thành", example = "50")
    @JsonView(Views.Public.class)
    private Integer pointsReward;
    
    @Schema(description = "Cho phép nghe lại không giới hạn", example = "true")
    @JsonView(Views.Public.class)
    private Boolean allowUnlimitedReplay;
    
    @Min(value = 1, message = "Số lần nghe phải >= 1")
    @Max(value = 10, message = "Số lần nghe phải <= 10")
    @Schema(description = "Số lần nghe tối đa", example = "3")
    @JsonView(Views.Public.class)
    private Integer maxReplayCount;
    
    @Schema(description = "Trạng thái kích hoạt", example = "true")
    @JsonView(Views.Public.class)
    private Boolean isActive;

    // ═══════════════════════════════════════════════════════════
    // READ-ONLY FIELDS
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
    
    @Schema(description = "Tên chủ đề", example = "Daily Life")
    @JsonView(Views.Public.class)
    private String topicName;
    
    @Schema(description = "Số lượng câu hỏi", example = "10")
    @JsonView(Views.Public.class)
    private Integer questionCount;
    
    @Schema(description = "Trình độ yêu cầu", example = "A2")
    @JsonView(Views.Public.class)
    private EnglishLevel requiredLevel;

    // ═══════════════════════════════════════════════════════════
    // USER PROGRESS
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
    
    @Schema(description = "Điểm phần trăm", example = "85.5")
    @JsonView(Views.Public.class)
    private Double scorePercentage;
    
    @Schema(description = "Số lần thử", example = "3")
    @JsonView(Views.Public.class)
    private Integer attempts;
    
    @Schema(description = "Số lần nghe", example = "5")
    @JsonView(Views.Public.class)
    private Integer playCount;
    
    @Schema(description = "Đã xem transcript chưa", example = "true")
    @JsonView(Views.Public.class)
    private Boolean hasViewedTranscript;
    
    @Schema(description = "Transcript đã mở khóa chưa", example = "true")
    @JsonView(Views.Public.class)
    private Boolean transcriptUnlocked;
    
    @Schema(description = "Số lần nghe còn lại", example = "2")
    @JsonView(Views.Public.class)
    private Integer remainingReplays;
    
    @Schema(description = "Thời gian hoàn thành", example = "2024-01-18T16:20:00")
    @JsonView(Views.Public.class)
    private LocalDateTime completedAt;

    // ═══════════════════════════════════════════════════════════
    // QUESTIONS
    // ═══════════════════════════════════════════════════════════
    
    @Deprecated
    @Schema(description = "⚠️ DEPRECATED: Dùng groupedQuestions thay thế")
    @JsonView(Views.Public.class)
    private List<QuestionResponseDTO> questions;
    
    @Schema(description = "✅ Câu hỏi theo cấu trúc Task (flat hoặc grouped)")
    @JsonView(Views.Public.class)
    private TaskGroupedQuestionsDTO groupedQuestions;

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════
    
    @Deprecated
    public ListeningLessonDTO withQuestions(List<QuestionResponseDTO> questions) {
        this.questions = questions;
        this.questionCount = questions != null ? questions.size() : 0;
        return this;
    }
    
    /**
     * ✅ NEW: Add grouped questions structure
     */
    public ListeningLessonDTO withGroupedQuestions(TaskGroupedQuestionsDTO groupedQuestions) {
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
    
    public ListeningLessonDTO withProgress(
            Boolean isCompleted, Double scorePercentage, Integer attempts,
            Integer playCount, Boolean hasViewedTranscript, LocalDateTime completedAt) {
        this.isCompleted = isCompleted;
        this.scorePercentage = scorePercentage;
        this.attempts = attempts;
        this.playCount = playCount;
        this.hasViewedTranscript = hasViewedTranscript;
        this.completedAt = completedAt;
        return this;
    }
    
    public ListeningLessonDTO withUnlockStatus(Boolean isUnlocked, Boolean isAccessible) {
        this.isUnlocked = isUnlocked;
        this.isAccessible = isAccessible;
        return this;
    }
    
    public ListeningLessonDTO withTranscriptInfo(
            Boolean transcriptUnlocked, Integer remainingReplays) {
        this.transcriptUnlocked = transcriptUnlocked;
        this.remainingReplays = remainingReplays;
        return this;
    }
}