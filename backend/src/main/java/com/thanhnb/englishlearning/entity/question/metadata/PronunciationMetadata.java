package com.thanhnb.englishlearning.entity.question.metadata;

import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PronunciationMetadata extends QuestionMetadata {
    private List<String> words; // Từ cần phát âm
    private String audioUrl; // URL của file âm thanh
    private List<String> categories;
    private List<Classification> correctClassifications;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Classification {
        private String word;
        private String category;
    }
}
