package com.thanhnb.englishlearning.exception;

public class OtpInvalidException extends BaseException {
    public OtpInvalidException(String message) {
        super(message, 400); // 400 Bad Request
    }
}
