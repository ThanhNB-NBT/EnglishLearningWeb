package com.thanhnb.englishlearning.enums;

public enum QuestionType {
    MULTIPLE_CHOICE("Trắc nghiệm"),
    FILL_BLANK("Điền vào chỗ trống"),
    TRANSLATE("Dịch Vi-En hoặc En-Vi"),
    TRUE_FALSE("Đúng/Sai"),
    VERB_FORM("Chia động từ"),
    SHORT_ANSWER("Trả lời ngắn"),
    SENTENCE_BUILDING("Xây dựng câu");

    private final String description;

    QuestionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
