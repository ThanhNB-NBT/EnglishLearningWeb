package com.thanhnb.englishlearning.dto.question.helper;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@Schema(description = "Kết quả kiểm tra câu trả lời")
public class QuestionResultDTO {
    @Schema(description = "ID câu hỏi", example = "101")
    private Long questionId;

    @Schema(description = "Nội dung câu hỏi")
    private String questionText;

    @Schema(description = "Câu trả lời của user")
    private String userAnswer;

    @Schema(description = "Đáp án đúng")
    private String correctAnswer;

    @Schema(description = "Đúng hay sai", example = "true")
    private Boolean isCorrect;

    @Schema(description = "Giải thích")
    private String explanation;

    @Schema(description = "Điểm đạt được", example = "5")
    private Integer points;

    @Schema(description = "Gợi ý khi sai")
    private String hint;

    @Schema(description = "Phản hồi sau khi kiểm tra câu trả lời")
    private String feedback;
}