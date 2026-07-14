package com.frigus.coreapi.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AppException {
    public NotFoundException(String message, String displayMessage) {
        super(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", message, displayMessage);
    }

    public NotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Não encontrado", "O recurso solicitado não foi encontrado");
    }
}
