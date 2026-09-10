package com.frigus.coreapi.dto.user;

import com.frigus.coreapi.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountTypeUpdateDto {
    @NotNull(message = "O tipo de conta não pode ser nulo")
    private AccountType accountType;
}
