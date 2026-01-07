package com.thanhnb.englishlearning.dto.question.response;

import com.thanhnb.englishlearning.dto.question.request.QuestionData;
import com.thanhnb.englishlearning.enums.ParentType;
import com.thanhnb.englishlearning.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;
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
@Schema(description = "Response DTO cho Question")
public class QuestionResponseDTO {

    @Schema(description = "ID câu hỏi", example = "1")
    @JsonView(Views.Public.class)
    private Long id;

    @Schema(description = "Loại parent", example = "GRAMMAR_LESSON")
    @JsonView(Views.Public.class)
    private ParentType parentType;

    @Schema(description = "ID của parent (lesson)", example = "10")
    @JsonView(Views.Public.class)
    private Long parentId;

    @Schema(description = "Nội dung câu hỏi", example = "What is the capital of Vietnam?")
    @JsonView(Views.Public.class)
    private String questionText;

    @Schema(description = "Loại câu hỏi", example = "MULTIPLE_CHOICE")
    @JsonView(Views.Public.class)
    private QuestionType questionType;

    @Schema(description = "Điểm số", example = "2")
    @JsonView(Views.Public.class)
    private Integer points;

    @Schema(description = "Thứ tự hiển thị", example = "1")
    @JsonView(Views.Public.class)
    private Integer orderIndex;

    @Schema(description = "ID của TaskGroup (null = standalone)", example = "5")
    @JsonView(Views.Public.class)
    private Long taskGroupId;

    @Schema(description = "Tên task group", example = "Task 1: Multiple Choice")
    @JsonView(Views.Public.class)
    private String taskGroupName;

    @Schema(description = "Hướng dẫn task", example = "Choose the correct answer (A, B, C or D)")
    @JsonView(Views.Public.class)
    private String taskInstruction;

    @Schema(description = "Thời gian tạo")
    @JsonView(Views.Public.class)
    private LocalDateTime createdAt;

    @Schema(description = "Dữ liệu chi tiết câu hỏi")
    @JsonView(Views.Public.class)
    private QuestionData data;
}