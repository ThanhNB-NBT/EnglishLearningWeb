package com.thanhnb.englishlearning.dto.grammar;

import com.thanhnb.englishlearning.enums.LessonType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tóm tắt thông tin của bài học ngữ pháp")
public class GrammarLessonSummaryDTO {
    @Schema(description = "ID của bài học", example = "1")
    private Long id;

    @Schema(description = "Tiêu đề của bài học", example = "Cách sử dụng thì hiện tại đơn")
    private String title;

    @Schema(description = "Loại của bài học (LÝ THUYẾT hoặc THỰC HÀNH)", example = "LÝ THUYẾT")
    private LessonType lessonType;

    @Schema(description = "Thứ tự của bài học", example = "1")
    private Integer orderIndex;

    @Schema(description = "Điểm yêu cầu để truy cập bài học", example = "0")
    private Integer pointsRequired;

    @Schema(description = "Điểm thưởng cho việc hoàn thành bài học", example = "10")
    private Integer pointsReward;

    @Schema(description = "Trạng thái của bài học", example = "true")
    private Boolean isActive;

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
}