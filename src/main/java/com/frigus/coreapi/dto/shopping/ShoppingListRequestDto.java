package com.frigus.coreapi.dto.shopping;

import com.frigus.coreapi.enums.ListStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListRequestDto {
    @NotNull
    private Integer stockId;

    private LocalDate date;

    @Builder.Default
    private ListStatus status = ListStatus.OPEN;
}
