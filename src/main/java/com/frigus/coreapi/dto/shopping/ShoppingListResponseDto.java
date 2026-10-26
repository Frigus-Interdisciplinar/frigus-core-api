package com.frigus.coreapi.dto.shopping;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record ShoppingListResponseDto(UUID id, Integer stockId, String name, LocalDate date, ListStatus status, String supplier, BigDecimal total, Instant completedAt, Instant createdAt, List<ShoppingItemResponseDto> items) { }
