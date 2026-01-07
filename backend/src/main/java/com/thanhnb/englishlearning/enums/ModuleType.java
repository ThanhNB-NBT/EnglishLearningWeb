package com.thanhnb.englishlearning.enums;

public enum ModuleType {
    GRAMMAR("Ngữ pháp"),
    LISTENING("Nghe"),
    READING("Đọc");

    private final String description;

    ModuleType(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
