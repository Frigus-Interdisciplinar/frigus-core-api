package com.frigus.coreapi.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String message, String displayMessage) {
        super(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", message, displayMessage);
    }

    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", "Não autorizado", "");
    }
}
