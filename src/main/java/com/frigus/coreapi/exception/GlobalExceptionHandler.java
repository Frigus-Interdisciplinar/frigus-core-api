package com.frigus.coreapi.exception;

import com.frigus.coreapi.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(BadRequestException ex) {
        log.warn("Bad Request: {}", ex.getMessage());
        return buildResponse(ex);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return buildResponse(ex);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponseDto> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: {}", ex.getMessage());
        return buildResponse(ex);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NotFoundException ex) {
        log.warn("Not Found: {}", ex.getMessage());
        return buildResponse(ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildResponse(ex);
    }

    @ExceptionHandler(InternalServerError.class)
    public ResponseEntity<ErrorResponseDto> handleInternalServerError(InternalServerError ex) {
        log.error("Internal Server Error: {}", ex.getMessage(), ex);
        return buildResponse(ex);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.error("Service Unavailable: {}", ex.getMessage(), ex);
        return buildResponse(ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneric(Exception ex) {
        log.error("Exceção não tratada: {}", ex.getMessage(), ex);

        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "Erro interno do servidor",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.",
                Instant.now()
        );
        return ResponseEntity.internalServerError().body(body);
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(AppException ex) {
        ErrorResponseDto body = new ErrorResponseDto(
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage(),
                ex.getDisplayMessage(),
                ex.getTimestamp()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
