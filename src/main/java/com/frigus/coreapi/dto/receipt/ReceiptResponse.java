package com.frigus.coreapi.dto.receipt;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.StoragePlace;
import com.frigus.coreapi.enums.UnitOfMeasure;
public record ReceiptResponse(UUID receiptId, String status, Invoice invoice, List<Item> items, List<String> warnings) {
 public record Invoice(String documentType, String accessKey, String issuerName, Instant issuedAt, BigDecimal totalAmount) {}
 public record Item(UUID lineId, String description, String normalizedProductName, String brand, Category category, StoragePlace storagePlace, UnitOfMeasure unitOfMeasure, Integer quantity, String unit, BigDecimal unitPrice, BigDecimal totalPrice, Double confidence, String evidence, MatchedProduct matchedProduct, LocalDate expireDate) {}
 public record MatchedProduct(Integer productId, String name) {}
 public record ImportedItem(UUID lineId, Integer stockProductId, Integer productId, Integer quantity) {}
 public record ConfirmationResponse(UUID receiptId, String status, List<ImportedItem> importedItems, List<UUID> ignoredLineIds) {}
}
