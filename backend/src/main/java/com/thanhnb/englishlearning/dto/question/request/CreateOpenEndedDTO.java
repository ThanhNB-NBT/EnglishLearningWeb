package com.thanhnb.englishlearning.dto.question.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO để TẠO MỚI câu hỏi mở (OPEN ENDED)")
public class CreateOpenEndedDTO extends QuestionData {
    
    @JsonView(Views.Admin.class)
    private String suggestedAnswer;

    @JsonView(Views.Public.class)
    private Integer timeLimitSeconds;

    @JsonView(Views.Public.class)
    private Integer minWord;
    
    @JsonView(Views.Public.class)
    private Integer maxWord;

}

    
