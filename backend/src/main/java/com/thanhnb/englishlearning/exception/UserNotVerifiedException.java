package com.thanhnb.englishlearning.exception;

public class UserNotVerifiedException extends BaseException {
    public UserNotVerifiedException(String message) {
        super(message, 403); // 403 Forbidden
    }
}
