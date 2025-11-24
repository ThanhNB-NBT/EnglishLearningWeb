package com.thanhnb.englishlearning.entity.question.metadata;

import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MatchingMetadata extends QuestionMetadata {
    private List<Pair> pairs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pair {
        private String left;
        private String right;
        private Integer order;
    }
}
