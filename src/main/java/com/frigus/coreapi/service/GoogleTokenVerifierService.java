package com.frigus.coreapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.UnauthorizedException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenVerifierService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${google.client-id:}")
    private String configuredClientId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleUserData {
        private String googleId;
        private String email;
        private String name;
        private String picture;
        private boolean emailVerified;
    }

    public GoogleUserData verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new BadRequestException("Token inválido", "O idToken do Google não foi fornecido.");
        }

        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken.trim();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new UnauthorizedException("Token do Google inválido", "Falha ao validar token junto ao Google.");
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            String sub = root.path("sub").asText(null);
            String email = root.path("email").asText(null);
            String name = root.path("name").asText(null);
            String picture = root.path("picture").asText(null);
            boolean emailVerified = root.path("email_verified").asBoolean(false)
                    || "true".equalsIgnoreCase(root.path("email_verified").asText());

            if (sub == null || email == null) {
                throw new UnauthorizedException("Token do Google incompleto", "O token não contém dados válidos de usuário.");
            }

            if (!emailVerified) {
                throw new UnauthorizedException("Email não verificado", "O e-mail da conta Google não está verificado.");
            }

            if (configuredClientId != null && !configuredClientId.isBlank()) {
                String aud = root.path("aud").asText("");
                if (!configuredClientId.equals(aud)) {
                    log.warn("Audience do Google ({}) difere do configurado ({})", aud, configuredClientId);
                }
            }

            return GoogleUserData.builder()
                    .googleId(sub)
                    .email(email)
                    .name(name != null && !name.isBlank() ? name : email.split("@")[0])
                    .picture(picture)
                    .emailVerified(emailVerified)
                    .build();

        } catch (HttpStatusCodeException e) {
            log.error("Erro HTTP ao validar idToken do Google: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new UnauthorizedException("Token do Google inválido ou expirado", "Não foi possível autenticar com o Google.");
        } catch (Exception e) {
            if (e instanceof UnauthorizedException || e instanceof BadRequestException) {
                throw (RuntimeException) e;
            }
            log.error("Erro inesperado na validação do token Google: {}", e.getMessage(), e);
            throw new UnauthorizedException("Erro na autenticação", "Falha interna ao validar credenciais do Google.");
        }
    }
}
