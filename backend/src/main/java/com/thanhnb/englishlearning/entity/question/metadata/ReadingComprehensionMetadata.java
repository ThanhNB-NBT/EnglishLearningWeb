package com.thanhnb.englishlearning.entity.question.metadata;

import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReadingComprehensionMetadata extends QuestionMetadata {
    private String passage;
    private List<Blanks> blanks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Blanks {
        private Integer position;
        private List<String> options;
        private String correctAnswer;
    }
    
}
