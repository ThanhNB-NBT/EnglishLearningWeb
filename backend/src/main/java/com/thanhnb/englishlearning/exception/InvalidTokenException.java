package com.thanhnb.englishlearning.exception;

public class InvalidTokenException extends BaseException {
    public InvalidTokenException(String message) {
        super(message, 401); // 401 Unauthorized
    }
}
