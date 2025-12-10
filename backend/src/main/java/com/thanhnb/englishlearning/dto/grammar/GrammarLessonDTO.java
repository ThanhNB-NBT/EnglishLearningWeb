package com.thanhnb.englishlearning.dto.grammar;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thanhnb.englishlearning.dto.question.request.CreateQuestionDTO;
import com.thanhnb.englishlearning.dto.question.response.QuestionResponseDTO;
import com.thanhnb.englishlearning.enums.LessonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Thông tin bài học ngữ pháp")
public class GrammarLessonDTO {

    // === BASIC INFO (Dùng cho cả Summary và Full) ===
    @Schema(description = "ID của bài học", example = "1")
    private Long id;

    @NotNull(message = "Topic ID không được để trống")
    @Schema(description = "ID của chủ đề", example = "1")
    private Long topicId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    @Schema(description = "Tiêu đề bài học", example = "How to use Present Simple")
    private String title;

    @NotNull(message = "Loại bài học không được để trống")
    @Schema(description = "Loại bài học", example = "THEORY")
    private LessonType lessonType;

    @NotNull(message = "Thứ tự không được để trống")
    @Min(value = 1, message = "Thứ tự phải >= 1")
    @Schema(description = "Thứ tự bài học", example = "1")
    private Integer orderIndex;

    @Min(value = 1, message = "Điểm thưởng phải >= 1")
    @Schema(description = "Điểm thưởng khi hoàn thành", example = "10")
    private Integer pointsReward = 10;

    @Min(value = 10, message = "Thời gian ước tính phải >= 10 giây")
    @Schema(description = "Thời gian ước tính (giây)", example = "30")
    private Integer timeLimitSeconds = 30;

    @Schema(description = "Trạng thái hoạt động", example = "true")
    private Boolean isActive = true;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    private Map<String, Object> metadata;

    @Schema(description = "Tên chủ đề")
    private String topicName;

    @Schema(description = "Số lượng câu hỏi", example = "10")
    private Integer questionCount;

    // === FULL DETAILS (Chỉ dùng khi load full) ===
    @Schema(description = "Nội dung bài học (HTML/Markdown)")
    private String content;

    @Schema(description = "Danh sách câu hỏi - dùng cho get endpoint")
    private List<QuestionResponseDTO> questions;

    @Schema(description = "Danh sách câu hỏi để tạo mới - dùng cho import hoặc admin tạo mới")
    private List<CreateQuestionDTO> createQuestions;

    // === USER PROGRESS (Chỉ có khi user logged in) ===
    @Schema(description = "Đã hoàn thành chưa", example = "false")
    private Boolean isCompleted;

    @Schema(description = "Có thể truy cập không", example = "true")
    private Boolean isAccessible;

    @Schema(description = "Đã mở khóa chưa", example = "true")
    private Boolean isUnlocked;

    @Schema(description = "Điểm của user", example = "85")
    private Integer userScore;

    @Schema(description = "Số lần thử", example = "2")
    private Integer userAttempts;

    // ===== STATIC FACTORY METHODS - TẠO DTO DỄ DÀNG =====

    /**
     * Tạo Summary DTO (cho danh sách)
     */
    public static GrammarLessonDTO summary(Long id, Long topicId, String title, 
            LessonType lessonType, Integer orderIndex, Integer pointsReward, 
            Boolean isActive, Integer questionCount) {
        GrammarLessonDTO dto = new GrammarLessonDTO();
        dto.setId(id);
        dto.setTopicId(topicId);
        dto.setTitle(title);
        dto.setLessonType(lessonType);
        dto.setOrderIndex(orderIndex);
        dto.setPointsReward(pointsReward);
        dto.setIsActive(isActive);
        dto.setQuestionCount(questionCount);
        return dto;
    }

    /**
     * Tạo Full DTO (cho chi tiết)
     */
    public static GrammarLessonDTO full(Long id, Long topicId, String title, 
            LessonType lessonType, String content, Integer orderIndex, 
            Integer pointsReward, Integer timeLimitSeconds, Boolean isActive, 
            LocalDateTime createdAt, Map<String, Object> metadata, String topicName) {
        GrammarLessonDTO dto = new GrammarLessonDTO();
        dto.setId(id);
        dto.setTopicId(topicId);
        dto.setTitle(title);
        dto.setLessonType(lessonType);
        dto.setContent(content);
        dto.setOrderIndex(orderIndex);
        dto.setPointsReward(pointsReward);
        dto.setTimeLimitSeconds(timeLimitSeconds);
        dto.setIsActive(isActive);
        dto.setCreatedAt(createdAt);
        dto.setMetadata(metadata);
        dto.setTopicName(topicName);
        return dto;
    }

    /**
     * Check if lesson has questions
     */
    public boolean hasQuestions() {
        return getQuestionCount() > 0;
    }

    /**
     * Get appropriate questions list based on context
     */
    public Object getQuestionsForContext(boolean isCreate) {
        return isCreate ? createQuestions : questions;
    }
}