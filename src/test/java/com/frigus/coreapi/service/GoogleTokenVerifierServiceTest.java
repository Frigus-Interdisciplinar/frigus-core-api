package com.frigus.coreapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frigus.coreapi.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleTokenVerifierServiceTest {

    @Test
    void reportsGoogleAsUnavailableWhenTokenVerificationCannotConnect() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));
        GoogleTokenVerifierService service = new GoogleTokenVerifierService(
                new ObjectMapper(),
                restTemplate
        );
        ReflectionTestUtils.setField(service, "configuredClientId", "test-client-id");

        assertThatThrownBy(() -> service.verify("fake-test-token"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Serviço de validação do Google indisponível");
    }
}
