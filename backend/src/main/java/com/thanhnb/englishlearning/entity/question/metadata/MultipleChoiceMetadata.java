package com.thanhnb.englishlearning.entity.question.metadata;

import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MultipleChoiceMetadata extends QuestionMetadata {
    private List<Option> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String text;
        private Boolean isCorrect;
        private Integer order;
    }
}
