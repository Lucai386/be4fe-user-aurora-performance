package com.be4fe_user_aurora_performance.exception;

import lombok.Getter;

@Getter
public class AuthenticationException extends RuntimeException {

    private final String errorCode;

    public AuthenticationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthenticationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
