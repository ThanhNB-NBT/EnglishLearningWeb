package com.thanhnb.englishlearning.dto.question.response;

import com.thanhnb.englishlearning.enums.ParentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response DTO cho Task Group")
public class TaskGroupResponseDTO {

    @Schema(description = "ID của task group", example = "1")
    private Long id;

    @Schema(description = "Loại parent", example = "GRAMMAR_LESSON")
    private ParentType parentType;

    @Schema(description = "ID của parent (lesson)", example = "10")
    private Long parentId;

    @Schema(description = "Tên task", example = "Task 1: Multiple Choice")
    private String taskName;

    @Schema(description = "Hướng dẫn task", example = "Choose the correct answer (A, B, C or D)")
    private String instruction;

    @Schema(description = "Thứ tự hiển thị", example = "1")
    private Integer orderIndex;

    @Schema(description = "Số lượng câu hỏi trong task", example = "5")
    private Integer questionCount;

    @Schema(description = "Tổng điểm của các câu hỏi", example = "10")
    private Integer totalPoints;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;
}