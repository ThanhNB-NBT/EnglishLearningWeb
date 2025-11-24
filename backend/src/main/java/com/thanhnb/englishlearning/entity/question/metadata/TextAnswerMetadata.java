package com.thanhnb.englishlearning.entity.question.metadata;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class TextAnswerMetadata extends QuestionMetadata {
    private String answer;
    private Boolean caseSensitive;
}
