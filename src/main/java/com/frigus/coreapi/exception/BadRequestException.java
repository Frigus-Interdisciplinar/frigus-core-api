package com.frigus.coreapi.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends AppException {
    public BadRequestException(String message, String displayMessage) {
        super(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", message, displayMessage
        );
    }

    public BadRequestException() {
        super(HttpStatus.BAD_REQUEST.value(), "BAD_EXCEPTION", "Erro ao fazer a requisição", "Ocorreu um erro ao processar sua requisição");
    }
}