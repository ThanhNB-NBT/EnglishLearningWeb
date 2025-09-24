package com.thanhnb.englishlearning.dto.grammar;

import com.thanhnb.englishlearning.enums.LessonType;
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
@Schema(description = "Chi tiết của bài học ngữ pháp")
public class GrammarLessonDTO {

    @Schema(description = "ID của bài học", example = "1")
    private Long id;

    @NotNull(message = "Topic Id không được để trống")
    @Schema(description = "ID của chủ đề", example = "1")
    private Long topicId;

    @NotBlank(message = "Tiêu đề bài học không được để trống")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 từ")
    @Schema(description = "Tiêu đề của bài học", example = "Cách sử dụng thì hiện tại đơn")
    private String title;

    @NotNull(message = "Loại bài không được để trống")
    @Schema(description = "Loại của bài học (LÝ THUYẾT hoặc THỰC HÀNH)", example = "LÝ THUYẾT")
    private LessonType lessonType;

    @Schema(description = "Nội dung của bài học (đối với bài lý thuyết)", example = "Giải thích về thì hiện tại đơn...")
    private String content;

    @NotNull(message = "Thứ tự không được để trống")
    @Min(value = 1, message = "Thứ tự phải lớn hơn 0")
    @Schema(description = "Thứ tự của bài học", example = "1")
    private Integer orderIndex;

    @Min(value = 0, message = "Điểm yêu cầu không được âm")
    @Schema(description = "Điểm yêu cầu để truy cập bài học", example = "0")
    private Integer pointsRequired = 0;

    @Min(value = 1, message = "Điểm thưởng phải lớn hơn 0")
    @Schema(description = "Điểm thưởng cho việc hoàn thành bài học", example = "10")
    private Integer pointsReward = 10;

    @Schema(description = "Trạng thái của bài học", example = "true")
    private Boolean isActive = true;

    @Schema(description = "Thời gian tạo", example = "2025-09-24T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Tên của chủ đề", example = "Thì hiện tại đơn")
    private String topicName;

    @Schema(description = "Danh sách các câu hỏi trong bài học (đối với bài thực hành)")
    private List<GrammarQuestionDTO> questions;

    @Schema(description = "Số lượng câu hỏi trong bài học", example = "10")
    private Integer questionCount;

    @Schema(description = "Trạng thái hoàn thành của bài học", example = "false")
    private Boolean isCompleted;

    @Schema(description = "Trạng thái truy cập của bài học", example = "true")
    private Boolean isAccessible;

    @Schema(description = "Trạng thái mở khóa của bài học", example = "true")
    private Boolean isUnlocked;

    @Schema(description = "Điểm của người dùng cho bài học", example = "80")
    private Integer userScore;

    @Schema(description = "Số lần thử nghiệm của người dùng", example = "2")
    private Integer userAttempts;

}