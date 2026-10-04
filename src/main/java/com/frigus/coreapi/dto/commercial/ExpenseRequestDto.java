package com.frigus.coreapi.dto.commercial;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record ExpenseRequestDto(@NotBlank @Size(max=255) String description, @NotBlank @Size(max=120) String category, @NotNull @DecimalMin(value="0.00",inclusive=false) @Digits(integer=10,fraction=2) BigDecimal amount, @NotNull LocalDate expenseDate, @Size(max=255) String supplier) { }
