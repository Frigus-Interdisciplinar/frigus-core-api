package com.frigus.coreapi.dto.moneysaving;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class MoneySavingDtos {
    private MoneySavingDtos() {}

    public record PurchaseLineInput(
            @Positive Long id,
            @Positive Integer stockId,
            @Positive Integer productId,
            LocalDate expireDate,
            @NotNull @Positive Integer quantity,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal unitPrice,
            @Size(max = 64) String category) {}

    public record PurchaseInput(
            @NotNull LocalDate purchaseDate,
            @NotBlank @Size(max = 32) String source,
            @Size(max = 160) String receiptReference,
            @PositiveOrZero Integer version,
            @NotEmpty List<@NotNull @Valid PurchaseLineInput> lines) {}
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
