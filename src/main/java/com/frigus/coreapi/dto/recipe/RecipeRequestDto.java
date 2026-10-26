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
    @jakarta.validation.constraints.Size(max=255)
    private String name;

    @jakarta.validation.constraints.Size(max=10000)
    private String description;
    @NotBlank @jakarta.validation.constraints.Size(max=20000)
    private String instructions;

    @Builder.Default
    private Boolean domesticOnly = true;

    @Builder.Default
    private Boolean active = true;
}
