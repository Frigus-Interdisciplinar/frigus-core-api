package com.frigus.coreapi.dto.subscription;

import com.frigus.coreapi.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusUpdateDto {

    @NotNull(message = "O status da assinatura é obrigatório")
    private SubscriptionStatus status;
}
