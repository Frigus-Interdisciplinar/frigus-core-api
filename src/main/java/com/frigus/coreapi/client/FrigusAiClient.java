package com.frigus.coreapi.client;

import com.frigus.coreapi.dto.ai.FrigusAiPromptPayload;
import com.frigus.coreapi.dto.ai.FrigusAiRecipeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class FrigusAiClient {

    private final RestClient restClient;
    private final String baseUrl;

    public FrigusAiClient(@Value("${FRIGUS_AI_BASE_URL}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
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
        } catch (RestClientException e) {
            log.warn("Serviço frigus_ai indisponível ou erro na chamada ({}). Acionando fallback.", e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao chamar frigus_ai: {}. Acionando fallback.", e.getMessage());
        }

        return generateFallbackRecipe(payload);
    }

    private FrigusAiRecipeResponse generateFallbackRecipe(FrigusAiPromptPayload payload) {
        String session = payload.getSessionId() != null && !payload.getSessionId().isBlank()
                ? payload.getSessionId()
                : UUID.randomUUID().toString();

        List<FrigusAiRecipeResponse.AiRecipeIngredientPayload> ingredientPayloads = new ArrayList<>();
        StringBuilder itemsSummary = new StringBuilder();

        if (payload.getAvailableItems() != null && !payload.getAvailableItems().isEmpty()) {
            for (int i = 0; i < Math.min(3, payload.getAvailableItems().size()); i++) {
                FrigusAiPromptPayload.StockItemPayload item = payload.getAvailableItems().get(i);
                ingredientPayloads.add(FrigusAiRecipeResponse.AiRecipeIngredientPayload.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(new BigDecimal("1.0"))
                        .unit("UNIT")
                        .required(true)
                        .build());
                if (i > 0) itemsSummary.append(", ");
                itemsSummary.append(item.getProductName());
            }
        }

        String recipeTitle = itemsSummary.length() > 0
                ? "Refeição Prática com " + itemsSummary
                : "Receita Sugerida Inteligente";

        String instructions = "1. Higienize e prepare os ingredientes.\n"
                + "2. Em uma panela ou frigideira em fogo médio, adicione um fio de azeite.\n"
                + "3. Cozinhe os ingredientes até dourarem no ponto desejado.\n"
                + "4. Ajuste o tempero a gosto com sal e ervas e sirva imediatamente.";

        return FrigusAiRecipeResponse.builder()
                .sessionId(session)
                .chatMessage("Preparei uma sugestão prática de receita aproveitando os ingredientes disponíveis no seu estoque!")
                .recipeName(recipeTitle)
                .description("Receita elaborada para otimizar o uso dos alimentos armazenados e reduzir o desperdício.")
                .instructions(instructions)
                .ingredients(ingredientPayloads)
                .build();
    }
}
