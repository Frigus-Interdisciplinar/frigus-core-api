package com.frigus.coreapi.dto.commercial;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record ExpenseResponseDto(UUID id, UUID groupId, String description, String category, BigDecimal amount, LocalDate expenseDate, String supplier, UUID shoppingListId, UUID createdBy) { }
