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
    @Size(max = 500, message = "Nội dung câu hỏi không được vượt quá 500 ký tự")
    @Schema(description = "Nội dung câu hỏi", example = "She ___ to school every day.")
    private String questionText;

    @NotNull(message = "Loại câu hỏi không được để trống")
    @Schema(description = "Loại câu hỏi", example = "FILL_BLANK")
    private QuestionType questionType;

    @Schema(description = "Đáp án đúng (Bắt buộc cho FILL_BLANK, TRANSLATE. Không cần cho MULTIPLE_CHOICE)", 
            example = "goes")
    private String correctAnswer;

    @Size(max = 1000, message = "Giải thích không được vượt quá 1000 ký tự")
    @Schema(description = "Giải thích đáp án")
    private String explanation;

    @Min(value = 1, message = "Điểm phải >= 1")
    @Max(value = 100, message = "Điểm phải <= 100")
    @Schema(description = "Điểm số", example = "5")
    private Integer points = 5;

    @Min(value = 1, message = "Thứ tự phải >= 1")
    @Schema(description = "Thứ tự câu hỏi", example = "1")
    private Integer orderIndex;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Các lựa chọn (Bắt buộc cho MULTIPLE_CHOICE, tối thiểu 2 lựa chọn)")
    private List<GrammarQuestionOptionDTO> options;

    @Schema(description = "Hiển thị đáp án đúng không", example = "false")
    private Boolean showCorrectAnswer = true;
}