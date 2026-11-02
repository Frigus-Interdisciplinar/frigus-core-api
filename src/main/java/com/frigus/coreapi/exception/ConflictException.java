package com.frigus.coreapi.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends AppException {
    public ConflictException(String message, String displayMessage) {
        super(HttpStatus.CONFLICT.value(), "CONFLICT", message, displayMessage);
    }

    public ConflictException() {
        super(HttpStatus.CONFLICT.value(), "CONFLICT", "Conflito", "Houve um conflito de dados");
    }
}
