package com.frigus.coreapi.exception;

import org.springframework.http.HttpStatus;

public class InternalServerError extends AppException {
    public InternalServerError(String message, String displayMessage) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", message, displayMessage);
    }

    public InternalServerError() {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", "Erro interno do servidor", "Ocorreu um erro interno no servidor");
    }
}
