package com.frigus.coreapi.exception;

public class ReceiptException extends AppException {
    public ReceiptException(int status, String code, String message, String displayMessage) {
        super(status, code, message, displayMessage);
    }
}
