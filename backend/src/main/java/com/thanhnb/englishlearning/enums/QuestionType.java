package com.thanhnb.englishlearning.enums;

public enum QuestionType {
    MULTIPLE_CHOICE("Trắc nghiệm"),
    FILL_BLANK("Điền vào chỗ trống"),
    TRUE_FALSE("Đúng/Sai"),
    VERB_FORM("Chia động từ"),
    SHORT_ANSWER("Trả lời ngắn"),
    ERROR_CORRECTION("Sửa lỗi"),
    MATCHING("Nối câu"),
    COMPLETE_CONVERSATION("Hoàn thành hội thoại"),
    PRONUNCIATION("Phát âm"),
    READING_COMPREHENSION("Đọc hiểu"),
    OPEN_ENDED("Tự luận"),
    SENTENCE_BUILDING("Xây dựng câu"),
    TEXT_ANSWER("Trả lời bằng văn bản");

    private final String description;

    QuestionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
