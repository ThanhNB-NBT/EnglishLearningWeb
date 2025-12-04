package com.thanhnb.englishlearning.enums;

public enum QuestionType {
    // === EXERCISE TYPES (Grammar/Vocab) ===
    MULTIPLE_CHOICE("Trắc nghiệm"),
    FILL_BLANK("Điền vào chỗ trống"),
    TRUE_FALSE("Đúng/Sai"),
    VERB_FORM("Chia động từ"),
    ERROR_CORRECTION("Sửa lỗi"),
    MATCHING("Nối câu"),
    SENTENCE_BUILDING("Xây dựng câu"),
    SENTENCE_TRANSFORMATION("Viết lại câu"),
    TEXT_ANSWER("Trả lời bằng văn bản"),
    COMPLETE_CONVERSATION("Hoàn thành hội thoại"),
    PRONUNCIATION("Phát âm"),
    OPEN_ENDED("Tự luận"),

    // === SKILL TYPES (4 kỹ năng) ===
    READING_COMPREHENSION("Đọc hiểu"),      // ParentType.READING
    LISTENING_COMPREHENSION("Nghe hiểu"),   // ParentType.LISTENING
    SPEAKING("Nói"),                        // ParentType.SPEAKING
    WRITING_ESSAY("Viết luận");
    
    private final String description;

    QuestionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
