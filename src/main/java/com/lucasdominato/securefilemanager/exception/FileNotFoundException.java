package com.lucasdominato.securefilemanager.exception;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(final String message) {
        super(message);
    }
}