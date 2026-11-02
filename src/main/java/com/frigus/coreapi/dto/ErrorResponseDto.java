package com.frigus.coreapi.dto;

import java.time.Instant;

import java.net.URI;

/**
 * RFC 9457 problem details with the legacy fields consumed by the BFF.
 */
public record ErrorResponseDto(
        URI type,
        String title,
        int status,
        String detail,
        String code,
        String message,
        String displayMessage,
        Instant timestamp
) {
    public ErrorResponseDto(int status, String code, String message, String displayMessage, Instant timestamp) {
        this(URI.create("about:blank"), message, status, displayMessage,
                code, message, displayMessage, timestamp);
    }
}
