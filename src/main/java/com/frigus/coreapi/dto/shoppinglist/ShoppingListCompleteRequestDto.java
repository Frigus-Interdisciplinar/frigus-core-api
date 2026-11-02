package com.frigus.coreapi.dto.shoppinglist;

import jakarta.validation.constraints.Size;

public record ShoppingListCompleteRequestDto(
        @Size(max = 255) String supplier) {}
