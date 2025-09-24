package com.thanhnb.englishlearning.dto.grammar;

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
@Schema(description = "Details of a grammar question")
public class GrammarQuestionDTO {
    @Schema(description = "ID của câu hỏi", example = "101")
    private Long id;

    @NotNull(message = "Lesson Id không được để trống")
    @Schema(description = "ID của bài học", example = "1")
    private Long lessonId;

    @NotBlank(message = "Câu hỏi không được để trống")
    @Schema(description = "Nội dung của câu hỏi", example = "Translate: Tôi hạnh phúc")
    private String questionText;

    @NotNull(message = "Loại câu hỏi không được để trống")
    @Schema(description = "Loại câu hỏi (MULTIPLE_CHOICE, FILL_BLANK, etc.)", example = "TRANSLATION_VI_EN")
    private QuestionType questionType;

    @NotBlank(message = "Đáp án không được để trống")
    @Schema(description = "Đáp án đúng cho câu hỏi", example = "I am happy")
    private String correctAnswer;

    @Schema(description = "Giải thích cho đáp án đúng", example = "Correct translation of 'Tôi hạnh phúc'")
    private String explanation;

    @Min(value = 1, message = "Điểm phải lớn hơn 0")
    @Schema(description = "Điểm cho câu hỏi", example = "5")
    private Integer points = 5;

    @Schema(description = "Thời gian tạo", example = "2025-09-24T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Danh sách các tùy chọn cho câu hỏi trắc nghiệm")
    private List<GrammarQuestionOptionDTO> options;

    @Schema(description = "Có hiển thị đáp án đúng hay không", example = "false")
    private Boolean showCorrectAnswer = true;
}