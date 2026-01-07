package com.thanhnb.englishlearning.dto.question.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi xây dựng câu (SENTENCE_BUILDING)")
public class CreateSentenceBuildingDTO extends QuestionData {

    @NotNull
    @Size(min = 2)
    @JsonView(Views.Public.class)
    private List<String> words;

    @NotBlank
    @JsonView(Views.Admin.class)
    private String correctSentence;

}
