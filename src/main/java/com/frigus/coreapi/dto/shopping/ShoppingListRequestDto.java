package com.frigus.coreapi.dto.shopping;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record ShoppingListRequestDto(@NotNull @Positive Integer stockId, @NotBlank @Size(max=255) String name, LocalDate date) { }
