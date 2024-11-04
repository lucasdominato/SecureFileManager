package com.lucasdominato.securefilemanager.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidJwtAuthenticationException extends AuthenticationException {

    public InvalidJwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}