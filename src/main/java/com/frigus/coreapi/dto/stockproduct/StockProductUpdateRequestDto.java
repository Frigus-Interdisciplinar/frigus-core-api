package com.frigus.coreapi.dto.stockproduct;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockProductUpdateRequestDto {
    @PositiveOrZero(message = "A quantidade mínima não pode ser negativa")
    private Integer minimalQuantity;

    @NotNull(message = "A data de validade é obrigatória")
    private LocalDate expireDate;
}
