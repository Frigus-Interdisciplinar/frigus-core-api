package com.frigus.coreapi.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsApplicationExceptionsToTheirDeclaredHttpResponse() {
        var badRequest = handler.handleBadRequest(new BadRequestException("invalid", "show invalid"));
        var unauthorized = handler.handleUnauthorized(new UnauthorizedException("unauthorized", "login"));
        var forbidden = handler.handleForbidden(new ForbiddenException("forbidden", "blocked"));
        var notFound = handler.handleNotFound(new NotFoundException("missing", "not found"));
        var conflict = handler.handleConflict(new ConflictException("conflict", "retry"));
        var unavailable = handler.handleServiceUnavailable(new ServiceUnavailableException("down", "later"));

        assertThat(badRequest.getStatusCode().value()).isEqualTo(400);
        assertThat(unauthorized.getStatusCode().value()).isEqualTo(401);
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);
        assertThat(notFound.getStatusCode().value()).isEqualTo(404);
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        assertThat(unavailable.getStatusCode().value()).isEqualTo(503);
        assertThat(badRequest.getBody().message()).isEqualTo("invalid");
    }

    @Test
    void mapsKnownAndUnexpectedServerErrors() {
        var known = handler.handleInternalServerError(new InternalServerError("failure", "try later"));
        var unexpected = handler.handleGeneric(new IllegalStateException("unexpected"));
        assertThat(known.getStatusCode().value()).isEqualTo(500);
        assertThat(known.getBody().displayMessage()).isEqualTo("try later");
        assertThat(unexpected.getStatusCode().value()).isEqualTo(500);
        assertThat(unexpected.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
