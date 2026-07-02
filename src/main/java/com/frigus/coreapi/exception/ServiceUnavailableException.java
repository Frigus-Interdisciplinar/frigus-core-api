package com.frigus.coreapi.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends AppException {
    public ServiceUnavailableException(String message, String displayMessage) {
        super(HttpStatus.SERVICE_UNAVAILABLE.value(), "SERVICE_UNAVAILABLE", message, displayMessage);
    }

    public ServiceUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE.value(), "SERVICE_UNAVAILABLE", "Serviço indisponível", "O serviço solicitado não está disponível no momento");
    }
}
