package com.frigus.coreapi.client;

import com.frigus.coreapi.dto.ai.FrigusAiPromptPayload;
import com.frigus.coreapi.dto.ai.FrigusAiRecipeResponse;
import com.frigus.coreapi.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Slf4j
@Component
public class FrigusAiClient {

    private final RestClient restClient;
    private final String baseUrl;

    public FrigusAiClient(
            @Value("${FRIGUS_AI_BASE_URL:http://localhost:8000}") String baseUrl,
            @Value("${FRIGUS_AI_API_KEY:}") String apiKey,
            @Value("${FRIGUS_AI_API_KEY_REQUIRED:false}") boolean apiKeyRequired) {
        if (apiKeyRequired && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalStateException("FRIGUS_AI_API_KEY is required");
        }
        this.baseUrl = baseUrl;
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }
        this.restClient = builder.build();
    }


    public record ChatCreateResponse(String chat_id, Integer stock_id) {}
    public record ChatMessageRequest(String content, Integer stock_id) {}
    public record ChatMessageResponse(String chat_id, String content) {}

    public FrigusAiRecipeResponse requestRecipeFromAi(FrigusAiPromptPayload payload) {
        try {
            String sessionId = payload.getSessionId();
            if (sessionId == null || sessionId.isBlank()) {
                ChatCreateResponse chat = restClient.post()
                        .uri("/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(ChatCreateResponse.class);
                if (chat != null && chat.chat_id() != null) {
                    sessionId = chat.chat_id();
                } else {
                    sessionId = UUID.randomUUID().toString();
                }
            }

            log.info("Enviando mensagem para frigus-ai chat {} (stockId: {})", sessionId, payload.getStockId());

            ChatMessageRequest request = new ChatMessageRequest(payload.getMessage(), payload.getStockId());
            ChatMessageResponse response = restClient.post()
                    .uri("/chats/{chatId}/messages", sessionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatMessageResponse.class);

            if (response != null && response.content() != null) {
                return FrigusAiRecipeResponse.builder()
                        .sessionId(response.chat_id() != null ? response.chat_id() : sessionId)
                        .chatMessage(response.content())
                        .build();
            }

            throw new ServiceUnavailableException("Resposta inválida da IA", "A inteligência artificial não retornou uma resposta válida.");
        } catch (RestClientException e) {
            log.error("Serviço frigus_ai indisponível ou erro na chamada HTTP");
            throw new ServiceUnavailableException("Serviço de IA indisponível", "O serviço de inteligência artificial está temporariamente indisponível. Tente novamente mais tarde.");
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao chamar frigus_ai");
            throw new ServiceUnavailableException("Erro ao processar IA", "Ocorreu uma falha ao comunicar com o assistente de inteligência artificial.");
        }
    }
}
