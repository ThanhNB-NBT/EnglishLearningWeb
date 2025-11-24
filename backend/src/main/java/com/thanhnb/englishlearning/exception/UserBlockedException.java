package com.thanhnb.englishlearning.exception;

public class UserBlockedException extends BaseException {
    public UserBlockedException(String message) {
        super(message, 403); // 403 Forbidden
    }
}
