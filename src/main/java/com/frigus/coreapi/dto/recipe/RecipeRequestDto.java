package com.frigus.coreapi.dto.recipe;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRequestDto {
    @NotBlank
    private String name;

    private String description;
    private String instructions;

    @Builder.Default
    private Boolean domesticOnly = true;

    @Builder.Default
    private Boolean active = true;
}
