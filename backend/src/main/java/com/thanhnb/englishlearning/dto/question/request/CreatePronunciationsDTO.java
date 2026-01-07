package com.thanhnb.englishlearning.dto.question.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi phát âm (PRONUNCIATIONS)")
public class CreatePronunciationsDTO extends QuestionData {

    @NotNull
    @Size(min = 2)
    @JsonView(Views.Public.class)
    private List<String> words;

    @NotNull
    @Size(min = 2)
    @JsonView(Views.Public.class)
    private List<String> categories;

    @NotNull
    @JsonView(Views.Admin.class)
    private List<ClassificationDTO> classifications;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationDTO {
    
        @NotBlank
        @JsonView(Views.Admin.class)
        private String word;

        @NotBlank
        @JsonView(Views.Admin.class)
        private String category;
    }
}
