package com.thanhnb.englishlearning.dto.question.request;

import java.util.Set;

import com.thanhnb.englishlearning.enums.*;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi điền văn bản (TEXT_ANSWER)")
public class CreateTextAnswerDTO extends CreateQuestionDTO {

    private static final Set<QuestionType> ALLOWED_TYPES = Set.of(
            QuestionType.FILL_BLANK,
            QuestionType.SHORT_ANSWER,
            QuestionType.VERB_FORM,
            QuestionType.ERROR_CORRECTION);

    private String hint;

    @NotBlank
    private String correctAnswer;

    private Boolean caseSensitive = false;
    @NotNull(message = "Question type không được null")
    @Schema(description = "Loại câu hỏi (FILL_BLANK, SHORT_ANSWER, VERB_FORM, ERROR_CORRECTION)", example = "FILL_BLANK")
    private QuestionType type;

    @Override
    public QuestionType getQuestionType() {
        validateType();
        return type;
    }

    private void validateType() {
        if (type == null) {
            throw new IllegalArgumentException(
                    "Question type không được null cho CreateTextAnswerDTO");
        }

        if (!ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                    String.format("Question type '%s' không được hỗ trợ cho CreateTextAnswerDTO. " +
                            "Chỉ chấp nhận: %s",
                            type, ALLOWED_TYPES));
        }
    }

    public static boolean isValidType(QuestionType type) {
        return ALLOWED_TYPES.contains(type);
    }
}
