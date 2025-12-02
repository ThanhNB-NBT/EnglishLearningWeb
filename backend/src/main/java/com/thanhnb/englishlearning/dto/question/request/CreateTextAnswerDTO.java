package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CreateTextAnswerDTO extends CreateQuestionDTO {
    
    @NotBlank(message = "Đáp án không được để trống")
    private String correctAnswer;
    
    private Boolean caseSensitive;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.TEXT_ANSWER;
    }
}
