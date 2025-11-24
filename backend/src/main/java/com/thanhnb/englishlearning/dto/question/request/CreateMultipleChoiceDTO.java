package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi trắc nghiệm (MULTIPLE_CHOICE)")
public class CreateMultipleChoiceDTO extends CreateQuestionDTO {
    
    private String hint;

    @NotNull @Size(min = 2)
    private List<OptionDTO> options;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.MULTIPLE_CHOICE;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Đáp án cho câu hỏi trắc nghiệm")
    public static class OptionDTO {
        @NotBlank
        @Schema(description = "Nội dung đáp án", example = "Đáp án A")
        private String text;

        private boolean isCorrect;
        @NotNull
        @Min(1)
        private Integer order;
    }
}