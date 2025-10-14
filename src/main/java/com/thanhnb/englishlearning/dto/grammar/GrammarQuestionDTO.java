package com.thanhnb.englishlearning.dto.grammar;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thanhnb.englishlearning.enums.QuestionType;
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
@Schema(description = "Câu hỏi ngữ pháp")
public class GrammarQuestionDTO {
    
    @Schema(description = "ID câu hỏi", example = "101")
    private Long id;

    @NotNull(message = "Lesson ID không được để trống")
    @Schema(description = "ID bài học", example = "1")
    private Long lessonId;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    @Schema(description = "Nội dung câu hỏi", example = "She ___ to school every day.")
    private String questionText;

    @NotNull(message = "Loại câu hỏi không được để trống")
    @Schema(description = "Loại câu hỏi", example = "FILL_BLANK")
    private QuestionType questionType;

    @NotBlank(message = "Đáp án không được để trống")
    @Schema(description = "Đáp án đúng", example = "goes")
    private String correctAnswer;

    @Schema(description = "Giải thích đáp án")
    private String explanation;

    @Min(value = 1, message = "Điểm phải >= 1")
    @Schema(description = "Điểm số", example = "5")
    private Integer points = 5;

    @Schema(description = "Thứ tự câu hỏi", example = "1")
    private Integer orderIndex;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Các lựa chọn (cho MULTIPLE_CHOICE)")
    private List<GrammarQuestionOptionDTO> options;

    @Schema(description = "Hiển thị đáp án đúng không", example = "false")
    private Boolean showCorrectAnswer = true;
}