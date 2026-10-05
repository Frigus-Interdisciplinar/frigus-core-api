package com.frigus.coreapi.dto.shopping;

import com.frigus.coreapi.enums.ListStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListResponseDto {
    private UUID id;
    private LocalDate date;
    private Integer stockId;
    private ListStatus status;
    private Instant createdAt;
}
