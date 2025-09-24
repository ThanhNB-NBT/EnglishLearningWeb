package com.thanhnb.englishlearning.dto.grammar;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResultDTO {

    @Schema(description = "ID câu hỏi", example = "101")
    private Long questionId;

    @Schema(description = "Nội dung câu hỏi", example = "Translate: Tôi hạnh phúc")
    private String questionText;

    @Schema(description = "Câu trả lời của người dùng", example = "I am happy")
    private String userAnswer;

    @Schema(description = "Câu trả lời đúng cho câu hỏi", example = "I am happy")
    private String correctAnswer;

    @Schema(description = "Trạng thái câu trả lời", example = "true")
    private Boolean isCorrect;

    @Schema(description = "Giải thích cho câu trả lời đúng", example = "Correct translation.")
    private String explanation;

    @Schema(description = "Điểm số cho câu hỏi", example = "10")
    private Integer points;

    @Schema(description = "Gợi ý cho các câu trả lời sai", example = "Kiểm tra lại ngữ pháp và từ vựng.")
    private String hint;
}
