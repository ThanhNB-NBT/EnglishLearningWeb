package com.thanhnb.englishlearning.enums;

public enum ParentType {
    GRAMMAR("Bài tập ngữ pháp"),      // → GrammarLesson
    VOCABULARY("Bài tập từ vựng"),    // → VocabularyLesson (nếu có)
    READING("Kỹ năng đọc"),           // → ReadingLesson
    LISTENING("Kỹ năng nghe"),        // → ListeningLesson
    SPEAKING("Kỹ năng nói"),          // → SpeakingTopic
    WRITING("Kỹ năng viết");          // → WritingTopic

    private final String description;

    ParentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
