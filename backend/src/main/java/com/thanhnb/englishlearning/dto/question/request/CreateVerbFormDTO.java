package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO cho câu hỏi Chia động từ")
public class CreateVerbFormDTO extends CreateQuestionDTO {

    @NotEmpty(message = "Danh sách động từ (blanks) không được để trống")
    @Valid
    private List<VerbBlankDTO> blanks;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.VERB_FORM;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerbBlankDTO {
        @NotNull(message = "Vị trí không được null")
        private Integer position;
        
        private String verb; // Động từ gốc
        
        @NotEmpty(message = "Phải có ít nhất 1 đáp án đúng")
        private List<String> correctAnswers;
    }
}