package com.frigus.coreapi.dto.moneysaving;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class MoneySavingDtos {
    private MoneySavingDtos() {}

    public record PurchaseLineInput(Long id, Integer stockId, Integer productId, LocalDate expireDate,
                                    Integer quantity, BigDecimal unitPrice, String category) {}
    public record PurchaseInput(LocalDate purchaseDate, String source, String receiptReference,
                                Integer version, List<PurchaseLineInput> lines) {}
    public record PurchaseLine(Long id, Integer stockKey, Integer stockProductId, String productName, String category,
                               String categoryGroup, String unitOfMeasure, LocalDate expireDate,
                               int quantity, BigDecimal unitPrice, BigDecimal total) {}
    public record Purchase(UUID id, LocalDate purchaseDate, String source, String receiptReference,
                           int version, BigDecimal total, List<PurchaseLine> lines) {}
    public record Metric(BigDecimal amount, long actions) {}
    public record CategoryTotal(String category, BigDecimal spent, BigDecimal saved, BigDecimal lost) {}
    public record MonthPoint(String month, BigDecimal spent, BigDecimal saved, BigDecimal lost) {}
    public record CategoryAmount(String category, BigDecimal amount) {}
    public record Summary(Metric spending, Metric saving, Metric loss, BigDecimal previousSpending,
                          BigDecimal spendingChangePercent, boolean hasFinancialData,
                          List<CategoryTotal> categories, List<MonthPoint> months) {}
    public record Event(String id, String kind, LocalDate date, String productName, String category,
                        String categoryGroup, String unitOfMeasure, int quantity, BigDecimal amount,
                        boolean verifiedValue, boolean includedInMetric, String reasonCode,
                        List<CategoryAmount> categoryAmounts) {}
    public record EventPage(List<Event> content, long totalElements, int number, int size, int totalPages) {}
}
