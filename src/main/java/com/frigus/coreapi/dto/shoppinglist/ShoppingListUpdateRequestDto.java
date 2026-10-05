package com.frigus.coreapi.dto.shoppinglist;

import com.frigus.coreapi.enums.ListStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListUpdateRequestDto {
    private LocalDate date;
    private ListStatus status;
}
