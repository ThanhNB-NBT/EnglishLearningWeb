package com.thanhnb.englishlearning.exception;

public class InvalidPasswordException extends BaseException {
    public InvalidPasswordException(String message) {
        super(message, 400); // 400 Bad Request
    }
}
