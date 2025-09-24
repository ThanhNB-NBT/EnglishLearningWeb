package com.thanhnb.englishlearning.dto.grammar;

import com.thanhnb.englishlearning.enums.EnglishLevel;
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
@Schema(description = "Chi tiết chủ đề ngữ pháp")
public class GrammarTopicDTO {

    @Schema(description = "ID của chủ đề", example = "1")
    private Long id;

    @NotBlank(message = "Tên chủ đề không được để trống")
    @Size(max = 100, message = "Tên chủ đề không được vượt quá 100 ký tự")
    @Schema(description = "Tên của chủ đề", example = "Thì hiện tại đơn")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 từ")
    @Schema(description = "Mô tả của chủ đề", example = "Học các kiến thức cơ bản về thì hiện tại đơn")
    private String description;

    @NotNull(message = "Level yêu cầu không được để trống")
    @Schema(description = "Level tiếng Anh yêu cầu", example = "BEGINNER")
    private EnglishLevel levelRequired;

    @NotNull(message = "Thứ tự không được để trống")
    @Min(value = 1, message = "Thứ tự phải lớn hơn 0")
    @Schema(description = "Thứ tự của chủ đề", example = "1")
    private Integer orderIndex;

    @Schema(description = "Trạng thái của chủ đề", example = "true")
    private Boolean isActive = true;

    @Schema(description = "Thời gian tạo", example = "2025-09-24T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Danh sách các bài học trong chủ đề")
    private List<GrammarLessonSummaryDTO> lessons;

    @Schema(description = "Số bài học đã hoàn thành của người dùng", example = "2")
    private Integer completedLessons;

    @Schema(description = "Tổng số bài học trong chủ đề", example = "5")
    private Integer totalLessons;

    @Schema(description = "Người dùng có thể truy cập chủ đề này không", example = "true")
    private Boolean isAccessible;
    
}