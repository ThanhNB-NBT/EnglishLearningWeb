package com.thanhnb.englishlearning.enums;

public enum QuestionType {
    MULTIPLE_CHOICE("Trắc nghiệm"),
    FILL_BLANK("Điền vào chỗ trống"),
    TRANSLATION_VI_EN("Dịch Vi-En"),
    TRANSLATION_EN_VI("Dịch En-Vi"),
    SENTENCE_BUILDING("Xây dựng câu");

    private final String description;

    QuestionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
