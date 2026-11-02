package com.frigus.coreapi.dto.notification;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdClickReportRequestDto {
    @NotNull(message = "O total de cliques é obrigatório")
    @Positive(message = "O total de cliques deve ser maior que zero")
    private Long totalClicks;
}
