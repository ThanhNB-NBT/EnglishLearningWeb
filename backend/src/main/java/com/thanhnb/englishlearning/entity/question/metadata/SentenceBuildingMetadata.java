package com.thanhnb.englishlearning.entity.question.metadata;

import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SentenceBuildingMetadata extends QuestionMetadata {
    private List<String> words; // Các từ cần sắp xếp
    private String correctSentence; // Câu đúng hoàn chỉnh
}
