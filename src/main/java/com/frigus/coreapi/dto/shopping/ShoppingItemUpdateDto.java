package com.frigus.coreapi.dto.shopping;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record ShoppingItemUpdateDto(@Positive Integer quantity, ProductListStatus status, @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal purchasedUnitPrice) { }
