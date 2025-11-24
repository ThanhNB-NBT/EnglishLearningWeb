package com.thanhnb.englishlearning.entity.question.metadata;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenEndedMetadata extends QuestionMetadata {
    private String suggestedAnswer;
    private String timeLimitSeconds;
    private Integer minWords;
    private Integer maxWords;
}
