package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.*;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi đúng/sai (TRUE_FALSE)")
public class CreateTrueFalseDTO extends CreateQuestionDTO {

    private String hint;

    @NotNull
    private Boolean correctAnswer;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.TRUE_FALSE;
    }
}
