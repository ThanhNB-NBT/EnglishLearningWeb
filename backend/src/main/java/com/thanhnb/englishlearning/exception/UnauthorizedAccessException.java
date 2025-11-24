package com.thanhnb.englishlearning.exception;

public class UnauthorizedAccessException extends BaseException {
    public UnauthorizedAccessException(String message) {
        super(message, 401); // 401 Unauthorized
    }
}
