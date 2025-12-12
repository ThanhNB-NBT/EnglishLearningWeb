package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.*;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi ghép đôi (MATCHING)")
public class CreateMatchingDTO extends CreateQuestionDTO {

    @NotNull
    @Size(min = 2)
    private List<PairDTO> pairs;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.MATCHING;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PairDTO {
        @NotBlank
        private String left;

        @NotBlank
        private String right;
        
        @NotNull
        @Min(1)
        private Integer order;
    }
}
