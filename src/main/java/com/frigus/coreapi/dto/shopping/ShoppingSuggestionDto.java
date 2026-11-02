package com.frigus.coreapi.dto.shopping;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record ShoppingSuggestionDto(com.frigus.coreapi.dto.product.ProductResponseDto product, int currentQuantity, int suggestedQuantity, String reason) { }
