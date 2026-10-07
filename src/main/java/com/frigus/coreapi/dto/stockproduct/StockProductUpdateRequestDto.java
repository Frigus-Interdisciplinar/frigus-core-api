package com.frigus.coreapi.dto.stockproduct;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockProductUpdateRequestDto {
    @PositiveOrZero(message = "A quantidade mínima não pode ser negativa")
    private Integer minimalQuantity;

    @DecimalMin(value = "0.00", inclusive = false, message = "O preço de aquisição deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "O preço de aquisição deve ter no máximo 8 dígitos inteiros e 2 decimais")
    private BigDecimal purchaseUnitPrice;

    @NotNull(message = "A data de validade é obrigatória")
    private LocalDate expireDate;
}
