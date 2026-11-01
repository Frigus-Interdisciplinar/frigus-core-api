package com.frigus.coreapi.dto.stock;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record StockSummaryDto(UUID groupId, int stockCount, long productCount, long itemCount,
 BigDecimal estimatedValue, long pendingShoppingItems, Map<UnitOfMeasure,Long> quantitiesByUnit,
 Map<StoragePlace,Long> itemsByStoragePlace, Map<UnitOfMeasure,Long> consumedThisWeek,
 LocalDate weekStart, List<com.frigus.coreapi.dto.stockproduct.StockProductResponseDto> nearExpiration) { }
