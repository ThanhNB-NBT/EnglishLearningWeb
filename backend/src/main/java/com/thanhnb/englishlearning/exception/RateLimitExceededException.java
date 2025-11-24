package com.thanhnb.englishlearning.exception;

public class RateLimitExceededException extends BaseException {
    public RateLimitExceededException(String message) {
        super(message, 429); // 429 Too Many Requests
    }
}
