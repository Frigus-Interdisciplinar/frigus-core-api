package com.frigus.coreapi.dto.stockproduct;

import com.frigus.coreapi.enums.Category;
import com.frigus.coreapi.enums.StoragePlace;
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

    private LocalDate expireDate;

    @jakarta.validation.constraints.Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
    @jakarta.validation.constraints.Pattern(regexp = "^$|.*\\S.*", message = "O nome não pode ficar vazio")
    private String name;

    private Category category;
    private StoragePlace storagePlace;

    @PositiveOrZero(message = "A quantidade não pode ser negativa")
    private Integer quantity;

    @PositiveOrZero(message = "A quantidade inicial não pode ser negativa")
    private Integer initialQuantity;

    @jakarta.validation.constraints.Size(max = 120)
    private String batch;

    public boolean isEmpty() {
        return minimalQuantity == null && expireDate == null && name == null && category == null
                && storagePlace == null && quantity == null && initialQuantity == null && batch == null;
    }
}
