package com.frigus.coreapi.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Component
public class BrevoEmailClient {
    private final RestClient restClient;
    private final String apiKey;
    private final Sender sender;

    public BrevoEmailClient(
            @Value("${brevo.api-url:https://api.brevo.com/v3}") String apiUrl,
            @Value("${brevo.api-key:}") String apiKey,
            @Value("${brevo.sender-email:}") String senderEmail,
            @Value("${brevo.sender-name:Frigus}") String senderName) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.sender = new Sender(senderName, senderEmail);
    }

    public void send(String to, String subject, String textContent) {
        if (apiKey.isBlank()) {
            throw new EmailDeliveryException("A chave da API do Brevo não está configurada");
        }
        if (sender.email().isBlank()) {
            throw new EmailDeliveryException("O remetente do Brevo não está configurado");
        }

        var request = new TransactionalEmailRequest(
                sender,
                List.of(new Recipient(to)),
                subject,
                textContent
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new EmailDeliveryException("Falha ao enviar e-mail pelo Brevo", exception);
        }
    }

    private record Sender(String name, String email) {}

    private record Recipient(String email) {}

    private record TransactionalEmailRequest(
            Sender sender,
            List<Recipient> to,
            String subject,
            String textContent) {}
}
