package com.frigus.coreapi.exception;

import lombok.Getter;
import java.time.Instant;

@Getter
public abstract class AppException extends RuntimeException {
    private final int status;
    private final String code; // * Futuramente implementar ENUM
    private final String message;
    private final String displayMessage;
    private final Instant timestamp;

    protected AppException(int status, String code, String message, String displayMessage) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
        this.displayMessage = displayMessage;
        this.timestamp = Instant.now();
    }
}