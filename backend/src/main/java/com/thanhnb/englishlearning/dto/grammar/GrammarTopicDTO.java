package com.thanhnb.englishlearning.dto.grammar;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Chi tiết chủ đề ngữ pháp")
public class GrammarTopicDTO {

    @Schema(description = "ID của chủ đề", example = "1")
    private Long id;

    @NotBlank(message = "Tên chủ đề không được để trống")
    @Size(max = 200, message = "Tên chủ đề không được vượt quá 200 ký tự")
    @Schema(description = "Tên của chủ đề", example = "Present Simple Tense")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    @Schema(description = "Mô tả của chủ đề")
    private String description;

    @NotNull(message = "Level yêu cầu không được để trống")
    @Schema(description = "Level tiếng Anh yêu cầu", example = "BEGINNER")
    private EnglishLevel levelRequired;

    @NotNull(message = "Thứ tự không được để trống")
    @Min(value = 1, message = "Thứ tự phải lớn hơn 0")
    @Schema(description = "Thứ tự của chủ đề", example = "1")
    private Integer orderIndex;

    @Schema(description = "Trạng thái hoạt động", example = "true")
    private Boolean isActive = true;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    // === User progress fields (chỉ dùng khi cần) ===
    @Schema(description = "Danh sách bài học (chỉ load khi cần)")
    private List<GrammarLessonDTO> lessons;

    @Schema(description = "Số bài học đã hoàn thành", example = "2")
    private Integer completedLessons;

    @Schema(description = "Tổng số bài học", example = "5")
    private Integer totalLessons;

    @Schema(description = "Có thể truy cập không", example = "true")
    private Boolean isAccessible;
}