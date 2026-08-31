package com.frigus.coreapi.dto.recipe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponseDto {
    private Integer id;
    private String name;
    private String description;
    private String instructions;
    private Boolean domesticOnly;
    private Boolean active;
    private Instant createdAt;
}
