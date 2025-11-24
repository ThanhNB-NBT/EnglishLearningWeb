package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi phát âm (PRONUNCIATIONS)")
public class CreatePronunciationsDTO extends CreateQuestionDTO {

    private String hint;

    @NotNull
    @Size(min = 2)
    private List<String> words;

    @NotNull
    @Size(min = 2)
    private List<String> categories;

    @NotNull
    private List<ClassificationDTO> classifications;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.PRONUNCIATION;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationDTO {
    
        @NotBlank
        private String word;

        @NotBlank
        private String category;
    }
}
