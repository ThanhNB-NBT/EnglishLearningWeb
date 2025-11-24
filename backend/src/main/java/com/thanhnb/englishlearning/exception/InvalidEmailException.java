package com.thanhnb.englishlearning.exception;

public class InvalidEmailException extends BaseException {
    public InvalidEmailException(String message) {
        super(message, 400); // 400 Bad Request
    }
}
