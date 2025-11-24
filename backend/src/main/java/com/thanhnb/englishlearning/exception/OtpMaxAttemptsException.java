package com.thanhnb.englishlearning.exception;

public class OtpMaxAttemptsException extends BaseException {
    public OtpMaxAttemptsException(String message) {
        super(message, 400); // 400 Bad Request
    }
}
