package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi xây dựng câu (SENTENCE_BUILDING)")
public class CreateSentenceBuildingDTO extends CreateQuestionDTO {

    private String hint;

    @NotNull
    @Size(min = 2)
    private List<String> words;

    @NotBlank
    private String correctSentence;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.SENTENCE_BUILDING;
    }
}
