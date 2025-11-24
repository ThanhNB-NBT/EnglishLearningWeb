package com.thanhnb.englishlearning.entity.question.metadata;

import lombok.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationMetadata extends QuestionMetadata {
    private List<String> conversationContext; // Đoạn hội thoại
    private List<String> options;
    private String correctAnswer;
}
