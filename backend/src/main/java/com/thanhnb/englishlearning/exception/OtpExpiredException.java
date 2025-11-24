package com.thanhnb.englishlearning.exception;

public class OtpExpiredException extends BaseException {
    public OtpExpiredException(String message) {
        super(message, 400); // 400 Bad Request
    }
    
}
