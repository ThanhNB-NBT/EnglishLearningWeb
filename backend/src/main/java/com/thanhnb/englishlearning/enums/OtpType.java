package com.thanhnb.englishlearning.enums;

public enum OtpType {
    VERIFY_EMAIL("Xác thực email đăng ký"),
    RESET_PASSWORD("Quên mật khẩu"),
    CHANGE_EMAIL("Thay đổi email"),
    CHANGE_PASSWORD("Thay đổi mật khẩu");

    private final String description;

    OtpType(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
