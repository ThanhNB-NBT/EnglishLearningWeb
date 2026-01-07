package com.thanhnb.englishlearning.event;

import com.thanhnb.englishlearning.enums.ModuleType;
import com.thanhnb.englishlearning.enums.QuestionType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class LessonCompletedEvent extends ApplicationEvent {

    private final Long userId;
    private final ModuleType module; // GRAMMAR, READING, LISTENING
    private final List<QuestionTrackingInfo> questionResults;

    private final Long topicId;
    private final String topicName;

    public LessonCompletedEvent(Object source, Long userId, ModuleType module, 
                                Long topicId, String topicName, // Thêm tham số
                                List<QuestionTrackingInfo> questionResults) {
        super(source);
        this.userId = userId;
        this.module = module;
        this.topicId = topicId;
        this.topicName = topicName;
        this.questionResults = questionResults;
    }

    // Class con (Inner class) để lưu kết quả từng câu
    @Getter
    public static class QuestionTrackingInfo {
        private final QuestionType type;
        private final boolean isCorrect;

        public QuestionTrackingInfo(QuestionType type, boolean isCorrect) {
            this.type = type;
            this.isCorrect = isCorrect;
        }
    }
}