package com.mtesazi.auth.exception;

public class UserAlreadyExistsException extends RuntimeException {


    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
