package com.thanhnb.englishlearning.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ✨ RECORD - Immutable, compact
 * Tự động generate constructor, getters, equals, hashCode, toString
 */
@Schema(description = "Kết quả kiểm tra câu trả lời")
public record QuestionResultDTO(
    @Schema(description = "ID câu hỏi", example = "101")
    Long questionId,

    @Schema(description = "Nội dung câu hỏi")
    String questionText,

    @Schema(description = "Câu trả lời của user")
    String userAnswer,

    @Schema(description = "Đáp án đúng")
    String correctAnswer,

    @Schema(description = "Đúng hay sai", example = "true")
    Boolean isCorrect,
    
    @Schema(description = "Giải thích")
    String explanation,

    @Schema(description = "Điểm đạt được", example = "5")
    Integer points,

    @Schema(description = "Gợi ý khi sai")
    String hint
) {}