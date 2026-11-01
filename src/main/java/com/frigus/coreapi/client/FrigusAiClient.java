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
import org.springframework.web.client.RestClientResponseException;
import java.util.UUID;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class FrigusAiClient {
    private static final int INVOICE_PRODUCT_BATCH_SIZE = 10;

    private final RestClient restClient;
    private final boolean configured;
    private final String apiKey;

    public FrigusAiClient(
            @Value("${FRIGUS_AI_BASE_URL:http://localhost:8000}") String baseUrl,
            @Value("${FRIGUS_AI_API_KEY:}") String apiKey,
            @Value("${FRIGUS_AI_API_KEY_REQUIRED:false}") boolean apiKeyRequired) {
        this.configured = apiKey != null && apiKey.length() >= 32;
        this.apiKey = apiKey;
        if (apiKeyRequired && !configured) {
            throw new IllegalStateException("FRIGUS_AI_API_KEY is required");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(110));
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        // Uvicorn serves HTTP/1.1; avoid a JDK h2c upgrade on the internal request.
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }
        this.restClient = builder.build();
    }


    public record CoreChatRequest(String userId, Integer stockId, String message, String sessionId,
                                  List<String> foodPreferences) {}
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
                        .header("X-API-Key", apiKey == null ? "" : apiKey)
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
        long started = System.nanoTime();
        Integer stockId = payload == null ? null : payload.getStockId();
        boolean existingSession = payload != null && payload.getSessionId() != null && !payload.getSessionId().isBlank();
        try {
            if (!configured || payload == null || payload.getUserId() == null || payload.getUserId().isBlank()) {
                log.warn("AI chat unavailable before HTTP: stockId={}, credentialsConfigured={}, userPresent={}",
                        stockId, configured, payload != null && payload.getUserId() != null && !payload.getUserId().isBlank());
                throw new ServiceUnavailableException("Integração de IA indisponível", "O assistente está temporariamente indisponível.");
            }
            log.debug("AI chat request: stockId={}, existingSession={}, preparedStockItems={}", stockId, existingSession,
                    payload.getAvailableItems() == null ? null : payload.getAvailableItems().size());
            CoreChatRequest request = new CoreChatRequest(payload.getUserId(), payload.getStockId(),
                    payload.getMessage(), payload.getSessionId(),
                    payload.getFoodPreferences() == null ? List.of() : payload.getFoodPreferences());
            FrigusAiRecipeResponse response = restClient.post()
                    .uri("/internal/core/chat")
                    .header("X-Core-Service-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FrigusAiRecipeResponse.class);

            if (response != null && response.getSessionId() != null && !response.getSessionId().isBlank() &&
                    response.getChatMessage() != null && !response.getChatMessage().isBlank() &&
                    response.getRecipeIds() != null && response.getRecipeIds().size() <= 10 &&
                    response.getRecipeIds().stream().allMatch(id -> id != null && id > 0)) {
                log.info("AI chat completed: stockId={}, existingSession={}, recipeCount={}, elapsedMs={}",
                        stockId, existingSession, response.getRecipeIds().size(), elapsedMillis(started));
                return response;
            }

            log.warn("Invalid AI chat response: stockId={}, sessionPresent={}, messagePresent={}, recipeIdsPresent={}, elapsedMs={}",
                    stockId, response != null && response.getSessionId() != null && !response.getSessionId().isBlank(),
                    response != null && response.getChatMessage() != null && !response.getChatMessage().isBlank(),
                    response != null && response.getRecipeIds() != null, elapsedMillis(started));
            throw new ServiceUnavailableException("Resposta inválida da IA", "A inteligência artificial não retornou uma resposta válida.");
        } catch (RestClientResponseException e) {
            Throwable diagnostic = diagnosticCause(e, 0);
            log.error("AI HTTP call failed: operation=/internal/core/chat, stockId={}, existingSession={}, upstreamStatus={}, elapsedMs={}",
                    stockId, existingSession, e.getStatusCode().value(), elapsedMillis(started), diagnostic);
            throw unavailable(diagnostic);
        } catch (RestClientException e) {
            Throwable diagnostic = diagnosticCause(e, 0);
            log.error("AI transport/decoding failed: operation=/internal/core/chat, stockId={}, existingSession={}, elapsedMs={}",
                    stockId, existingSession, elapsedMillis(started), diagnostic);
            throw unavailable(diagnostic);
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            Throwable diagnostic = diagnosticCause(e, 0);
            log.error("Unexpected AI client failure: stockId={}, existingSession={}, elapsedMs={}",
                    stockId, existingSession, elapsedMillis(started), diagnostic);
            ServiceUnavailableException failure = new ServiceUnavailableException("Erro ao processar IA",
                    "Ocorreu uma falha ao comunicar com o assistente de inteligência artificial.");
            failure.initCause(diagnostic);
            throw failure;
        }
    }

    private static long elapsedMillis(long started) { return (System.nanoTime() - started) / 1_000_000; }

    private static ServiceUnavailableException unavailable(Throwable diagnostic) {
        ServiceUnavailableException failure = new ServiceUnavailableException("Serviço de IA indisponível",
                "O serviço de inteligência artificial está temporariamente indisponível. Tente novamente mais tarde.");
        failure.initCause(diagnostic);
        return failure;
    }

    /** Preserve failure classes, frames and causal chain without logging response bodies, tokens or prompts. */
    private static Throwable diagnosticCause(Throwable failure, int depth) {
        RuntimeException diagnostic = new RuntimeException(failure.getClass().getName());
        diagnostic.setStackTrace(failure.getStackTrace());
        if (failure.getCause() != null && failure.getCause() != failure && depth < 8)
            diagnostic.initCause(diagnosticCause(failure.getCause(), depth + 1));
        return diagnostic;
    }
}
