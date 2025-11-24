package com.thanhnb.englishlearning.exception;

public class UserNotFoundException extends BaseException {
    public UserNotFoundException(String message) {
        super(message, 404); // 404 Not Found
    }
    
}
