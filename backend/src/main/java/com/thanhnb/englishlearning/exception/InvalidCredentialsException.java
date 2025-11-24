package com.thanhnb.englishlearning.exception;

public class InvalidCredentialsException extends BaseException {
    public InvalidCredentialsException(String message) {
        super(message, 401); // 401 Unauthorized
    }
}
