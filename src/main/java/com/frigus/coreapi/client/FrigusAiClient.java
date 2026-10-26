package com.frigus.coreapi.client;

import com.frigus.coreapi.dto.ai.FrigusAiPromptPayload;
import com.frigus.coreapi.dto.ai.FrigusAiRecipeResponse;
import com.frigus.coreapi.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class FrigusAiClient {
    private static final int INVOICE_PRODUCT_BATCH_SIZE = 10;

    private final RestClient restClient;
    private final String baseUrl;

    public FrigusAiClient(
            @Value("${FRIGUS_AI_BASE_URL:http://localhost:8000}") String baseUrl,
            @Value("${FRIGUS_AI_API_KEY:}") String apiKey) {
        this.baseUrl = baseUrl;
        // Uvicorn serves HTTP/1.1; avoid a JDK h2c upgrade on the internal request.
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(110));
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }
        this.restClient = builder.build();
    }


    public record ChatCreateResponse(String chat_id, Integer stock_id) {}
    public record ChatMessageRequest(String content) {}
    public record ChatMessageResponse(String chat_id, String content) {}
    public record InvoiceProductInput(int lineNumber, String description, Double quantity, String unit, Double unitPrice, Double totalPrice, String evidence) {}
    public record InvoiceExtractionRequest(String documentType, String state, String locale, List<InvoiceProductInput> products) {}
    public record InvoiceItem(int lineNumber, String description, String productName, String brand, Double quantity, String unit, String unitOfMeasure, Double unitPrice, Double totalPrice, String category, String storagePlace, Double confidence, String evidence) {}
    public record InvoiceExtraction(String accessKey, String issuerName, String issuedAt, Double totalAmount, List<InvoiceItem> items, List<String> warnings) {}

    public InvoiceExtraction extractInvoice(List<InvoiceProductInput> products) {
        try {
            var items = new java.util.ArrayList<InvoiceItem>();
            var warnings = new java.util.ArrayList<String>();
            for (int start = 0; start < products.size(); start += INVOICE_PRODUCT_BATCH_SIZE) {
                var batch = products.subList(start, Math.min(start + INVOICE_PRODUCT_BATCH_SIZE, products.size()));
                var result = restClient.post().uri("/v1/invoices/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new InvoiceExtractionRequest("NFCE", "SP", "pt-BR", batch))
                        .retrieve().body(InvoiceExtraction.class);
                if (result == null || result.items() == null) throw new IllegalStateException("Resposta de extração vazia");
                items.addAll(result.items());
                if (result.warnings() != null) warnings.addAll(result.warnings());
            }
            return new InvoiceExtraction(null, null, null, null, items, warnings);
        } catch (RestClientException | IllegalStateException e) {
            log.warn("Falha ao extrair NFC-e via frigus-ai: {}", e.getMessage());
            throw new ServiceUnavailableException("Falha na extração da nota", "A extração da nota está temporariamente indisponível.");
        }
    }

    public FrigusAiRecipeResponse requestRecipeFromAi(FrigusAiPromptPayload payload) {
        try {
            String sessionId = payload.getSessionId();
            if (sessionId == null || sessionId.isBlank()) {
                ChatCreateResponse chat = restClient.post()
                        .uri("/v1/chats")
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

            ChatMessageRequest request = new ChatMessageRequest(payload.getMessage());
            ChatMessageResponse response = restClient.post()
                    .uri("/v1/chats/{chatId}/messages", sessionId)
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
            log.error("Serviço frigus_ai indisponível ou erro na chamada HTTP: {}", e.getMessage(), e);
            throw new ServiceUnavailableException("Serviço de IA indisponível", "O serviço de inteligência artificial está temporariamente indisponível. Tente novamente mais tarde.");
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao chamar frigus_ai: {}", e.getMessage(), e);
            throw new ServiceUnavailableException("Erro ao processar IA", "Ocorreu uma falha ao comunicar com o assistente de inteligência artificial.");
        }
    }
}
