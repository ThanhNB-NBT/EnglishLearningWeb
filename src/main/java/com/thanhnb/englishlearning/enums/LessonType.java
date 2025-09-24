package com.thanhnb.englishlearning.enums;

public enum LessonType {
    THEORY("Lý thuyết"), 
    PRACTICE("Thực hành");

    private final String description;

    LessonType(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
