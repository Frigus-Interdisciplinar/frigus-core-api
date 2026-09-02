package com.frigus.coreapi.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateRequestDto {
    @NotBlank(message = "O nome do grupo é obrigatório")
    private String name;
    private String bannerPicture;
}
