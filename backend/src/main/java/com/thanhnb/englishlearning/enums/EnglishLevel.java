package com.thanhnb.englishlearning.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EnglishLevel {
    A1, A2, B1, B2, C1, C2;
    
    // Helper để so sánh level (ví dụ check xem user có đủ level không)
    public boolean isAtLeast(EnglishLevel requiredLevel) {
        return this.ordinal() >= requiredLevel.ordinal();
    }

    @JsonCreator
    public static EnglishLevel fromString(String value) {
        try {
            return EnglishLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid English level: " + value);
        }
    }
}