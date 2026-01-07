package com.thanhnb.englishlearning.dto.question.request;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonView;
import com.thanhnb.englishlearning.config.Views;

@Data
public abstract class QuestionData {
    
    @JsonView(Views.Public.class)
    private String explanation;
}
