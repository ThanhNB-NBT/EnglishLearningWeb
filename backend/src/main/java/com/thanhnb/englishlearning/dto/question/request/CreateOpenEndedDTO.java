package com.thanhnb.englishlearning.dto.question.request;

import com.thanhnb.englishlearning.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi mở (OPEN ENDED)")
public class CreateOpenEndedDTO extends CreateQuestionDTO {

    private String hint;
    private String suggestedAnswer;
    private Integer timeLimitSeconds;
    private Integer minWord;
    private Integer maxWord;

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.OPEN_ENDED;
    }
}

    
