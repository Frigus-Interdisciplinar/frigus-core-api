package com.frigus.coreapi.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends AppException {
    public ForbiddenException(String message, String displayMessage) {
        super(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", message, displayMessage);
    }

    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Probido", "Acesso negado");
    }
}

