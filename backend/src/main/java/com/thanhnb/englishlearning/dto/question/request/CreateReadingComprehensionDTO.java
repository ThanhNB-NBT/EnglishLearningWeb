package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi đọc hiểu (READING COMPREHENSION)")
public class CreateReadingComprehensionDTO extends CreateQuestionDTO {

    private String hint;

    @NotBlank
    private String passage;

    @NotNull
    @Size(min = 1)
    private List<BlankDTO> blanks;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.READING_COMPREHENSION;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlankDTO {
        @NotNull
        @Min(1)
        private Integer position;

        @NotNull
        @Size(min = 2)
        private List<String> options;

        @NotBlank
        private String correctAnswer;
    }
    
}
