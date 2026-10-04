package com.frigus.coreapi.dto.recipe;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.frigus.coreapi.enums.*;
public record IngredientAvailabilityDto(Integer productId, String productName, BigDecimal requiredQuantity, int availableQuantity, String unit, boolean required, boolean available) { }
