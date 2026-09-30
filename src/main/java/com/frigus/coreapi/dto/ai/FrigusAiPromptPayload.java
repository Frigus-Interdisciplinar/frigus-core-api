package com.frigus.coreapi.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FrigusAiPromptPayload {

    private String userId;
    private String message;
    private String sessionId;
    private Integer stockId;
    private List<StockItemPayload> availableItems;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItemPayload {
        private Integer productId;
        private String productName;
        private String category;
        private Integer quantity;
        private LocalDate expireDate;
        private String status;
    }
}
