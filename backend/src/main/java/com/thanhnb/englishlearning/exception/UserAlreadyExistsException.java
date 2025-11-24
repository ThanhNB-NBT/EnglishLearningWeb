package com.thanhnb.englishlearning.exception;

public class UserAlreadyExistsException extends BaseException {
    public UserAlreadyExistsException(String message) {
        super(message, 409); // 409 Conflict
    }
    
}
