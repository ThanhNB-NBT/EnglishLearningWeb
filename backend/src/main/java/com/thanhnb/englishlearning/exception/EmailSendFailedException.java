package com.thanhnb.englishlearning.exception;

public class EmailSendFailedException extends BaseException {
    public EmailSendFailedException(String message) {
        super(message, 500); // 500 Internal Server Error
    }
}
